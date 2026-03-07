package org.example;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.ArrayList;
import java.util.List;

public class BankToolsB {

    public List<String> callLog = new ArrayList<>();

    // Variáveis para simular cenários
    private int withdrawFailCount = Integer.MAX_VALUE;
    private boolean depositFails = false;
    private int withdrawCount = 0;

    public void setupScenario(boolean depositFails, int withdrawFailCount) {
        this.depositFails = depositFails;
        this.withdrawFailCount = withdrawFailCount;
        this.withdrawCount = 0;
        this.callLog.clear();
    }

    public void resetState() {
        this.withdrawCount = 0;
        this.callLog.clear();
    }

    @Tool("Execute an operation in an account with a given value "
            + "and return if the operation was successful or not")
    public boolean executeOperation(
            @P("WITHDRAW if the operation is a withdraw,"
                    + "DEPOSIT if the operation is a deposit,"
                    + "TAX to charge the value of a tax from an account,"
                    + "RETURN to return the value of a failed operation,"
                    + "PAYMENT to perform a payment") OperationType type,
            @P("account number") String accountNumber,
            @P("value to be used in the operation") double value) {

        callLog.add(String.format("executeOperation(%s, %s, %.2f)", type, accountNumber, value));
        boolean success = true;

        switch (type) {
            case WITHDRAW:
                withdrawCount++;
                success = (withdrawCount < withdrawFailCount);
                break;
            case DEPOSIT:
                success = !depositFails;
                break;
            case TAX:
            case RETURN:
            case PAYMENT:
                success = true; // Assumindo sucesso
                break;
        }

        System.out.println(String.format("[ToolB] executeOperation(%s, %s, %.2f) -> %s", type, accountNumber, value, success));
        return success;
    }
}
