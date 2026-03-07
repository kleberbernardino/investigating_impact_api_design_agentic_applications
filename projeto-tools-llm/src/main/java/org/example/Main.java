package org.example;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

import java.util.*;

public class Main {

    interface Assistant {
        String chat(@MemoryId String memoryId, @UserMessage String userMessage);
    }

    private static final String OPENAI_MODEL_NAME = "mistral:7b";

    private static final int EXECUTIONS_PER_SCENARIO = 10;

    public class PromptBuilder {

        private static final String SYSTEM_MESSAGE = """
        You are a bank execution agent.
        
        Your task is to fulfill the user's request by calling the tools.
        Use the result of each operation to decide the next step.
        Execute operations in the required order until the task is complete.
        Only perform the required tool calls.
        Don't write down the logic, only call the tools.
        """;

        public static String build(String userPrompt) {
            return SYSTEM_MESSAGE + "\n\nPrompt to be executed:\n" + userPrompt;
        }
    }


    public static void main(String[] args) {

        String prompt1 = "Execute the following operations in order and one at a time.\n" +
                "\n" +
                "1. Withdraw 1000 from account BC12345.\n" +
                "2. Deposit 1000 into account ND87632.\n" +
                "\n" +
                "If both operations succeed:\n" +
                "- Charge 1.50 from account BC12345.\n" +
                "\n" +
                "If any operation fails:\n" +
                "- Undo any completed operation.\n" +
                "- Do not charge the tax for the operation.\n" +
                "- Stop and execute nothing else.";

        String prompt2 = "Execute the following operations in order and one at a time.\n" +
                "\n" +
                "1. Withdraw 500 from account BC3456A five times one at time.\n" +
                "2. Stop immediately if a withdrawal fails\n" +
                "\n" +
                "If the 5 executed withdrawals succeed:\n" +
                "- Deposit the total successfully withdrawn amount of 2500 into account FG62495S.\n" +
                "- Charge a tax equal to 10% of the deposited amount of 2500 from account FG62495S.\n" +
                "\n" +
                "If any withdrawal fails:\n" +
                "- Only consider the withdrawals that succeeded before the failure.\n" +
                "- Deposit the total of these successful withdrawals into account FG62495S.\n" +
                "- Charge a tax equal to 10% of that deposited amount.\n" +
                "- Do not execute any additional withdrawals beyond this point.\n" +
                "- Stop and execute nothing else.";

        String prompt3 = "Execute the following operations in order and one at a time.\n" +
                "\n" +
                "1. Withdraw 600 from account AG7340H.\n" +
                "2. Withdraw 700 from account TG23986Q.\n" +
                "\n" +
                "If both withdrawals succeed:\n" +
                "- Deposit the total withdrawn amount of 1300 into account WS2754T.\n" +
                "- Perform a payment of 1200 from account WS2754T.\n" +
                "\n" +
                "If any withdrawal fails:\n" +
                "- Return the operation amount to the account it came from for the successfully withdrawn.\n" +
                "- Stop and execute nothing else.";

        // === TOOLS ===
        BankToolsA toolsA_conf1 = new BankToolsA();
        BankToolsB toolsB_conf2 = new BankToolsB();
        BankToolsA toolsA_conf3 = new BankToolsA();
        BankToolsB toolsB_conf3 = new BankToolsB();
        BankToolsA toolsA_conf4 = new BankToolsA();
        BankToolsB toolsB_conf4 = new BankToolsB();

        // === ASSISTANTS ===
        Assistant assistant1 = createAssistant(toolsA_conf1);
        Assistant assistant2 = createAssistant(toolsB_conf2);
        Assistant assistant3 = createAssistant(toolsA_conf3, toolsB_conf3);
        Assistant assistant4 = createAssistant(toolsB_conf4, toolsA_conf4);

        System.out.println("=========================================================");
        System.out.println("Iniciando bateria de testes com o modelo: " + OPENAI_MODEL_NAME);
        System.out.println("Execuções por cenário: " + EXECUTIONS_PER_SCENARIO);
        System.out.println("=========================================================");

        // --- Execução da Bateria de Testes ---

        // CONF1: BankToolsA
        runAllScenariosForConfig("CONF1: BankToolsA", assistant1, List.of(toolsA_conf1),
                prompt1, prompt2, prompt3);

        // CONF2: BankToolsB
        runAllScenariosForConfig("CONF2: BankToolsB", assistant2, List.of(toolsB_conf2),
                prompt1, prompt2, prompt3);

        // CONF3: BankToolsA, BankToolsB
        runAllScenariosForConfig("CONF3: BankToolsA, BankToolsB", assistant3, List.of(toolsA_conf3, toolsB_conf3),
                prompt1, prompt2, prompt3);

        // CONF4: BankToolsB, BankToolsA
        runAllScenariosForConfig("CONF4: BankToolsB, BankToolsA", assistant4, List.of(toolsB_conf4, toolsA_conf4),
                prompt1, prompt2, prompt3);

        System.out.println("\n=========================================================");
        System.out.println("--- Bateria de testes concluída. ---");
        System.out.println("=========================================================");

    }

    // === EXECUÇÃO DE TODOS OS CENÁRIOS ===
    private static void runAllScenariosForConfig(String configName, Assistant assistant, List<Object> tools,
                                                 String p1, String p2, String p3) {

        System.out.println("\n#########################################################");
        System.out.println("Iniciando Configuração: " + configName);
        System.out.println("#########################################################");

        // --- PROMPT 1 ---
        // P1A: Saque OK, Depósito OK
        setupTools(tools, false, Integer.MAX_VALUE);
        runSingleExperiment(configName + " / PROMPT 1 / CENÁRIO A", assistant, tools, p1);

        // P1B: Saque OK, Depósito Falha
        setupTools(tools, true, Integer.MAX_VALUE);
        runSingleExperiment(configName + " / PROMPT 1 / CENÁRIO B", assistant, tools, p1);

        // --- PROMPT 2 ---
        // P2A: 5 Saques OK
        setupTools(tools, false, Integer.MAX_VALUE);
        runSingleExperiment(configName + " / PROMPT 2 / CENÁRIO A", assistant, tools, p2);

        // P2B: 3 Saques OK, 4º Saque Falha
        setupTools(tools, false, 4);
        runSingleExperiment(configName + " / PROMPT 2 / CENÁRIO B", assistant, tools, p2);

        // --- PROMPT 3 ---
        // P3A: Ambos Saques OK
        setupTools(tools, false, Integer.MAX_VALUE);
        runSingleExperiment(configName + " / PROMPT 3 / CENÁRIO A", assistant, tools, p3);

        // P3B: 1º Saque OK, 2º Saque Falha
        setupTools(tools, false, 2);
        runSingleExperiment(configName + " / PROMPT 3 / CENÁRIO B", assistant, tools, p3);
    }

    private static void setupTools(List<Object> tools, boolean depositFails, int withdrawFailsAt) {
        for (Object tool : tools) {
            if (tool instanceof BankToolsA) {
                ((BankToolsA) tool).setupScenario(depositFails, withdrawFailsAt);
            } else if (tool instanceof BankToolsB) {
                ((BankToolsB) tool).setupScenario(depositFails, withdrawFailsAt);
            }
        }
    }


    // === EXECUÇÃO INDIVIDUAL ===
    private static void runSingleExperiment(String testName, Assistant assistant, List<Object> tools, String prompt) {
        System.out.println("\n--- " + testName + " ---");

        int correctRuns = 0;
        Map<String, Integer> responseLogs = new HashMap<>();
        List<String> acceptanceCriteria = getAcceptanceCriteria(testName);

        for (int i = 1; i <= EXECUTIONS_PER_SCENARIO; i++) {
            String memoryId = UUID.randomUUID().toString();
            System.out.println("\n[Execução " + i + "]");

            try {
                String finalPrompt = PromptBuilder.build(prompt);
                String response = assistant.chat(memoryId, finalPrompt);
                System.out.println(": " + response);

                String combinedLogs = getLogsAndResetTools(tools);
                System.out.println("[Log de Chamadas]: " + (combinedLogs.isEmpty()? "Nenhuma chamada" : combinedLogs));

                if (acceptanceCriteria.contains(combinedLogs)) {
                    correctRuns++;
                }

                responseLogs.put(combinedLogs, responseLogs.getOrDefault(combinedLogs, 0) + 1);

            } catch (Exception e) {
                System.out.println(": " + e.getMessage());
                e.printStackTrace();

                getLogsAndResetTools(tools);
            }
        }

        System.out.println("\n---------------------------------------------------------");
        System.out.println("");
        System.out.println("CORRETUDE: " + correctRuns + "/" + EXECUTIONS_PER_SCENARIO + " execuções corretas.");
        System.out.println("CONSISTÊNCIA E ABORDAGEM:");
        if (responseLogs.isEmpty()) {
            System.out.println("  Nenhuma execução completada (Verificar ERROS acima).");
        }
        for (Map.Entry<String, Integer> entry : responseLogs.entrySet()) {
            String log = entry.getKey().isEmpty()? "Nenhuma chamada" : entry.getKey();
            boolean isCorrect = acceptanceCriteria.contains(log);
            System.out.println("  - - " + (isCorrect? "(CORRETO)" : "(INCORRETO)"));
            System.out.println("    Log: " + log);
        }
        System.out.println("---------------------------------------------------------");
    }

    private static String getLogsAndResetTools(List<Object> tools) {
        StringBuilder logs = new StringBuilder();
        for (Object tool : tools) {
            if (tool instanceof BankToolsA) {
                BankToolsA t = (BankToolsA) tool;
                if (t.callLog!= null &&!t.callLog.isEmpty()) {
                    logs.append("BankToolsA: ").append(t.callLog.toString()).append(" ");
                }
                t.resetState();
            } else if (tool instanceof BankToolsB) {
                BankToolsB t = (BankToolsB) tool;
                if (t.callLog!= null &&!t.callLog.isEmpty()) {
                    logs.append("BankToolsB: ").append(t.callLog.toString()).append(" ");
                }
                t.resetState();
            }
        }
        return logs.toString().trim();
    }

    private static List<String> getAcceptanceCriteria(String testName) {
        List<String> criteria = new ArrayList<>();

        // === CRITÉRIOS DE ACEITAÇÃO PARA BANKTOOLSA ===
        String P1A_A = "BankToolsA: [withdraw(BC12345, 1000,00), deposit(ND87632, 1000,00), taxes(BC12345, 1,50)]";
        String P1B_A = "BankToolsA: [withdraw(BC12345, 1000,00), deposit(ND87632, 1000,00), returnValue(BC12345, 1000,00)]";
        String P2A_A = "BankToolsA: [withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), deposit(FG62495S, 2500,00), taxes(FG62495S, 250,00)]";
        String P2B_A = "BankToolsA: [withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), withdraw(BC3456A, 500,00), deposit(FG62495S, 1500,00), taxes(FG62495S, 150,00)]";
        // P3A - Ordem 1
        String P3A_A1 = "BankToolsA: [withdraw(AG7340H, 600,00), withdraw(TG23986Q, 700,00), deposit(WS2754T, 1300,00), payment(WS2754T, 1200,00)]";
        // P3A - Ordem 2 (Inversa, também correta)
        String P3A_A2 = "BankToolsA: [withdraw(TG23986Q, 700,00), withdraw(AG7340H, 600,00), deposit(WS2754T, 1300,00), payment(WS2754T, 1200,00)]";
        // P3B - Ordem 1 (Falha no segundo saque)
        String P3B_A1 = "BankToolsA: [withdraw(AG7340H, 600,00), withdraw(TG23986Q, 700,00), returnValue(AG7340H, 600,00)]";
        // P3B - Ordem 2 (Falha no segundo saque)
        String P3B_A2 = "BankToolsA: [withdraw(TG23986Q, 700,00), withdraw(AG7340H, 600,00), returnValue(TG23986Q, 700,00)]";

        // === CRITÉRIOS DE ACEITAÇÃO PARA BANKTOOLSB ===
        String P1A_B = "BankToolsB: [executeOperation(WITHDRAW, BC12345, 1000,00), executeOperation(DEPOSIT, ND87632, 1000,00), executeOperation(TAX, BC12345, 1,50)]";
        String P1B_B = "BankToolsB: [executeOperation(WITHDRAW, BC12345, 1000,00), executeOperation(DEPOSIT, ND87632, 1000,00), executeOperation(RETURN, BC12345, 1000,00)]";
        String P2A_B = "BankToolsB: [executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(DEPOSIT, FG62495S, 2500,00), executeOperation(TAX, FG62495S, 250,00)]";
        String P2B_B = "BankToolsB: [executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(WITHDRAW, BC3456A, 500,00), executeOperation(DEPOSIT, FG62495S, 1500,00), executeOperation(TAX, FG62495S, 150,00)]";
        // P3A - Ordem 1
        String P3A_B1 = "BankToolsB: [executeOperation(WITHDRAW, AG7340H, 600,00), executeOperation(WITHDRAW, TG23986Q, 700,00), executeOperation(DEPOSIT, WS2754T, 1300,00), executeOperation(PAYMENT, WS2754T, 1200,00)]";
        // P3A - Ordem 2 (Inversa)
        String P3A_B2 = "BankToolsB: [executeOperation(WITHDRAW, TG23986Q, 700,00), executeOperation(WITHDRAW, AG7340H, 600,00), executeOperation(DEPOSIT, WS2754T, 1300,00), executeOperation(PAYMENT, WS2754T, 1200,00)]";
        // P3B - Ordem 1
        String P3B_B1 = "BankToolsB: [executeOperation(WITHDRAW, AG7340H, 600,00), executeOperation(WITHDRAW, TG23986Q, 700,00), executeOperation(RETURN, AG7340H, 600,00)]";
        // P3B - Ordem 2
        String P3B_B2 = "BankToolsB: [executeOperation(WITHDRAW, TG23986Q, 700,00), executeOperation(WITHDRAW, AG7340H, 600,00), executeOperation(RETURN, TG23986Q, 700,00)]";


        // --- LÓGICA DE PREENCHIMENTO DA LISTA ---
        if (testName.startsWith("CONF1")) { // Apenas ToolsA
            if (testName.contains("PROMPT 1 / CENÁRIO A")) criteria.add(P1A_A);
            if (testName.contains("PROMPT 1 / CENÁRIO B")) criteria.add(P1B_A);
            if (testName.contains("PROMPT 2 / CENÁRIO A")) criteria.add(P2A_A);
            if (testName.contains("PROMPT 2 / CENÁRIO B")) criteria.add(P2B_A);
            if (testName.contains("PROMPT 3 / CENÁRIO A")) { criteria.add(P3A_A1); criteria.add(P3A_A2); }
            if (testName.contains("PROMPT 3 / CENÁRIO B")) { criteria.add(P3B_A1); criteria.add(P3B_A2); }
        } else if (testName.startsWith("CONF2")) { // Apenas ToolsB
            if (testName.contains("PROMPT 1 / CENÁRIO A")) criteria.add(P1A_B);
            if (testName.contains("PROMPT 1 / CENÁRIO B")) criteria.add(P1B_B);
            if (testName.contains("PROMPT 2 / CENÁRIO A")) criteria.add(P2A_B);
            if (testName.contains("PROMPT 2 / CENÁRIO B")) criteria.add(P2B_B);
            if (testName.contains("PROMPT 3 / CENÁRIO A")) { criteria.add(P3A_B1); criteria.add(P3A_B2); }
            if (testName.contains("PROMPT 3 / CENÁRIO B")) { criteria.add(P3B_B1); criteria.add(P3B_B2); }
        } else if (testName.startsWith("CONF3") || testName.startsWith("CONF4")) { // ToolsA ou ToolsB
            if (testName.contains("PROMPT 1 / CENÁRIO A")) { criteria.add(P1A_A); criteria.add(P1A_B); }
            if (testName.contains("PROMPT 1 / CENÁRIO B")) { criteria.add(P1B_A); criteria.add(P1B_B); }
            if (testName.contains("PROMPT 2 / CENÁRIO A")) { criteria.add(P2A_A); criteria.add(P2A_B); }
            if (testName.contains("PROMPT 2 / CENÁRIO B")) { criteria.add(P2B_A); criteria.add(P2B_B); }
            if (testName.contains("PROMPT 3 / CENÁRIO A")) { criteria.add(P3A_A1); criteria.add(P3A_A2); criteria.add(P3A_B1); criteria.add(P3A_B2); }
            if (testName.contains("PROMPT 3 / CENÁRIO B")) { criteria.add(P3B_A1); criteria.add(P3B_A2); criteria.add(P3B_B1); criteria.add(P3B_B2); }
        }

        return criteria;
    }

    // === CRIA ASSISTANT COM OLLAMA ===
    private static Assistant createAssistant(Object... tools) {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(OPENAI_MODEL_NAME)
                .temperature(0.0)
                .topP(1.0)
                .build();

        ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(model) .tools(tools)
                .chatMemoryProvider(memoryProvider)
                .build(); }

}
