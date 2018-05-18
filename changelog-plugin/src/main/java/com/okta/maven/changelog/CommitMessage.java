package com.okta.maven.changelog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitMessage {

    private final String rawMessage;
    private final String type;
    private final String shortMessage;

    private CommitMessage(String type, String shortMessage, String rawMessage) {
        this.rawMessage = rawMessage;
        this.type = type;
        this.shortMessage = shortMessage;
    }

    public String getType() {
        return type;
    }

    public String getSubject() {
        return shortMessage;
    }

    public static CommitMessage parse(String commitMessage) {

        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return null;
        }

        Pattern prefixRegex = Pattern.compile("^(?<prefix>\\w+):\\s+(?<subject>.+)");
        Pattern trailerRegex = Pattern.compile("^changelog:\\s+(?<type>.+)", Pattern.MULTILINE);

        Matcher prefixMatcher = prefixRegex.matcher(commitMessage);

        String type = null;
        String shortMessage = null;

        if (prefixMatcher.find()) {
            type = prefixMatcher.group("prefix");
            shortMessage = prefixMatcher.group("subject");
        } else {
            Matcher trailerMatcher = trailerRegex.matcher(commitMessage);
            if (trailerMatcher.find()) {
                type = trailerMatcher.group("type");
            }
            shortMessage = commitMessage.split("\\r?\\n")[0];
        }

        return new CommitMessage(type, shortMessage, commitMessage);
    }

}
