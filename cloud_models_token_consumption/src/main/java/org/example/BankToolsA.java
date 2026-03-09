package org.example;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.ArrayList;
import java.util.List;

public class BankToolsA {

    // Lista para registrar chamadas
    public List<String> callLog = new ArrayList<>();

    // Variáveis para simular cenários
    private int withdrawFailCount = Integer.MAX_VALUE;
    private boolean depositFails = false;
    private int withdrawCount = 0;

    // Método para configurar o cenário
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


    @Tool("Withdraw a value from an account "
            + "and return if the operation was successfull or not")
    public boolean withdraw(
            @P("account number") String accountNumber,
            @P("value to be withdraw") double value) {

        callLog.add(String.format("withdraw(%s, %.2f)", accountNumber, value));

        withdrawCount++;
        boolean success = (withdrawCount < withdrawFailCount);

        System.out.println(String.format("[ToolA] withdraw(%s, %.2f) -> %s", accountNumber, value, success));
        return success;
    }

    @Tool("Deposit the value into an account "
            + "and return if the operation was successfull or not")
    public boolean deposit(
            @P("account number") String accountNumber,
            @P("value to be deposited") double value) {

        callLog.add(String.format("deposit(%s, %.2f)", accountNumber, value));

        boolean success = !depositFails;

        System.out.println(String.format("[ToolA] deposit(%s, %.2f) -> %s", accountNumber, value, success));
        return success;
    }

    @Tool("Perform a payment with a value using the money from an account "
            + "and return if the operation was successfull or not")
    public boolean payment(
            @P("account number") String accountNumber,
            @P("value of the payment") double value) {

        callLog.add(String.format("payment(%s, %.2f)", accountNumber, value));
        System.out.println(String.format("[ToolA] payment(%s, %.2f) -> true", accountNumber, value));
        return true; // Assumindo sucesso
    }

    @Tool("Charge the value of a tax from the account "
            + "and return if the operation was successfull or not")
    public boolean taxes(
            @P("account number") String accountNumber,
            @P("value of the tax") double value) {

        callLog.add(String.format("taxes(%s, %.2f)", accountNumber, value));
        System.out.println(String.format("[ToolA] taxes(%s, %.2f) -> true", accountNumber, value));
        return true; // Assumindo sucesso
    }

    @Tool("Return a value of a failed operation to an account "
            + "and return if the operation was successfull or not")
    public boolean returnValue(
            @P("account number") String accountNumber,
            @P("value to be returned") double value) {

        callLog.add(String.format("returnValue(%s, %.2f)", accountNumber, value));
        System.out.println(String.format("[ToolA] returnValue(%s, %.2f) -> true", accountNumber, value));
        return true; // Assumindo sucesso
    }
}
