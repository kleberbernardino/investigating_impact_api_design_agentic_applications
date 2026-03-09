package org.example;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

public class TokenUsageListener implements ChatModelListener {

    private static int inputTokens = 0;
    private static int outputTokens = 0;

    @Override
    public void onRequest(ChatModelRequestContext context) {
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {

        TokenUsage usage = context.chatResponse().tokenUsage();

        if (usage != null) {
            inputTokens += usage.inputTokenCount();
            outputTokens += usage.outputTokenCount();
        }
    }

    public static void reset() {
        inputTokens = 0;
        outputTokens = 0;
    }

    public static int getInputTokens() {
        return inputTokens;
    }

    public static int getOutputTokens() {
        return outputTokens;
    }

    public static int getTotalTokens() {
        return inputTokens + outputTokens;
    }
}