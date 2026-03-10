package org.example.sharedprompts.domain.prompt.application.port.out.llm;

import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;

/** JSON Schema 기반 Constrained Decoding 아웃바운드 포트 */
public interface ConstrainedDecodingPort {

    String generateConstrained(String prompt, OutputContract outputContract);

    boolean validate(String output, OutputContract outputContract);
}
