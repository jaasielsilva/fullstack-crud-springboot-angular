package com.clientes_api.task.support;

import java.text.Normalizer;

public final class TaskBranchSupport {

    private TaskBranchSupport() {
    }

    public static String suggestBranch(Long taskId, String title) {
        String normalized = title == null ? "tarefa" : Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "tarefa";
        }
        if (slug.length() > 40) {
            slug = slug.substring(0, 40).replaceAll("-+$", "");
        }
        return "feature/TASK-" + taskId + "-" + slug;
    }
}
