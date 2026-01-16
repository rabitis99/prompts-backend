package org.example.sharedprompts.domain.report.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.domain.report.enums.ReportStatus;
import org.example.sharedprompts.domain.report.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.sharedprompts.domain.report.QReport.report;

@RequiredArgsConstructor
public class CustomReportRepositoryImpl implements CustomReportRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Report> findAllReports(Pageable pageable) {
        return searchInternal(null, null, null, pageable);
    }

    @Override
    public Page<Report> findReportsByStatus(ReportStatus status, Pageable pageable) {
        return searchInternal(status, null, null, pageable);
    }

    @Override
    public Page<Report> findReportsByType(ReportType reportType, Pageable pageable) {
        return searchInternal(null, reportType, null, pageable);
    }

    @Override
    public Page<Report> findReportsByStatusAndType(ReportStatus status, ReportType reportType, Pageable pageable) {
        return searchInternal(status, reportType, null, pageable);
    }

    @Override
    public Page<Report> findReportsByReporterId(Long reporterId, Pageable pageable) {
        return searchInternal(null, null, reporterId, pageable);
    }

    /**
     * 공통 2-step 페이징 + fetchJoin
     */
    private Page<Report> searchInternal(ReportStatus status, ReportType reportType, Long reporterId, Pageable pageable) {
        BooleanExpression where = buildWhereCondition(status, reportType, reporterId);
        
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(report.id)
                .from(report)
                .where(where)
                .orderBy(report.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(report.count())
                .from(report)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회
        // reporter는 필수이므로 innerJoin, 나머지는 nullable이므로 leftJoin
        List<Report> content = queryFactory
                .selectFrom(report)
                .innerJoin(report.reporter).fetchJoin()  // 필수 필드
                .leftJoin(report.processor).fetchJoin()  // nullable (처리 전에는 null)
                .leftJoin(report.prompt).fetchJoin()     // nullable (댓글 신고인 경우 null)
                .leftJoin(report.comment).fetchJoin()    // nullable (프롬프트 신고인 경우 null)
                .where(report.id.in(ids))
                .orderBy(report.createdAt.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 동적 where 조건 생성
     * QueryDSL의 null 안전 조합 활용
     */
    private BooleanExpression buildWhereCondition(ReportStatus status, ReportType reportType, Long reporterId) {
        return statusEq(status)
                .and(reportTypeEq(reportType))
                .and(reporterIdEq(reporterId));
    }

    private BooleanExpression statusEq(ReportStatus status) {
        return status != null ? report.status.eq(status) : null;
    }

    private BooleanExpression reportTypeEq(ReportType reportType) {
        return reportType != null ? report.reportType.eq(reportType) : null;
    }

    private BooleanExpression reporterIdEq(Long reporterId) {
        return reporterId != null ? report.reporter.id.eq(reporterId) : null;
    }
}

