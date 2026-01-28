package org.example.sharedprompts.domain.tag.enums;

import org.example.sharedprompts.domain.tag.count.TagCountMetrics;

public enum TagCountOperation {

    INCREASE {
        @Override
        public void record(TagCountMetrics metrics) {
            metrics.getIncreaseCounter().increment();
        }
    },
    DECREASE {
        @Override
        public void record(TagCountMetrics metrics) {
            metrics.getDecreaseCounter().increment();
        }
    };

    public abstract void record(TagCountMetrics metrics);
}