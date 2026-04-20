package managerAgent.workflow.review;

import org.springframework.stereotype.Component;

@Component
public class QualityReviewer {

    public String review(String taskName, String output) {
        if (output == null || output.isBlank()) {
            return "REJECT: empty output";
        }

        if (output.length() < 60) {
            return "WARN: output is too short, quality may be low";
        }

        if (output.toLowerCase().contains("unavailable") || output.toLowerCase().contains("fallback")) {
            return "WARN: degraded output detected";
        }

        return "PASS: output quality baseline satisfied";
    }
}
