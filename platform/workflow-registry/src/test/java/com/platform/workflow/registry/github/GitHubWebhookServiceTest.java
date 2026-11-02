package com.platform.workflow.registry.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GitHubWebhookServiceTest {

    @Test
    void parseLinkedIssues_findsFixesReference() {
        List<Integer> issues = GitHubWebhookService.parseLinkedIssues("This change Fixes #42 and closes #99");
        assertEquals(List.of(42, 99), issues);
    }

    @Test
    void parseLinkedIssues_emptyWhenMissing() {
        assertTrue(GitHubWebhookService.parseLinkedIssues("no link here").isEmpty());
    }
}
