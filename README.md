# Investigating the Impact of API Design Techniques for Tools in Agentic Applications
Investigating the Impact of API Design Techniques for Tools in Agentic Applications

## Implementation
The experimental framework was implemented in Java to ensure strong typing, explicit tool schema definitions, and deterministic orchestration control. The implementation was structured around four main classes: BankToolsA, BankToolsB, OperationType, and Main. Together, these components form a modular architecture that separates the tool abstraction layer (BankToolsA/B), the model interaction layer (LangChain4J integration), and the experiment control layer (Main orchestration and validation) as detailed below.

Four exposure configurations were evaluated: the first, CONF1, used only BankToolsA; the second, CONF2, used only BankToolsB; the third, CONF3, used BankToolsA followed by BankToolsB; and the fourth, CONF4, used BankToolsB followed by BankToolsA. CONF3 and CONF4 were designed to evaluate tool-selection bias introduced by declaration order.

## Repository Structure

```
src/
└── main/
      └── java/
           └── .../org.example/
                 ├── BankToolsA.java
                 ├── BankToolsB.java
                 ├── OperationType.java
                 └── Main.java
└── pom.xml
```

## Requirements
- Java 17+
- Maven 4+
- Ollama

### Variables
OPENAI_MODEL_NAME = ˜Enter Model Name Selected˜

EXECUTIONS_PER_SCENARIO = ˜Enter the Execution Per Scenario˜

## Prompt Engineering and Experimental Scenarios

Structured prompts were designed to explicitly define the execution order of operations, clearly specify the success and failure conditions for each step, eliminate ambiguity in rollback logic, and constrain the model’s reasoning space to reduce unintended behaviors. In total, three structured prompts were defined, with a system prompt set as the default message to eliminate variability caused by instruction changes. The system prompt was implemented as follows:

--START--

You are a bank execution agent.

Your task is to fulfill the user's request by calling the available tools.
Use the result of each operation to decide the next step.
Execute operations in the required order until the task is complete.
Only perform the required tool calls.

--END--

### Prompt 1 - Conditional Transfer with Tax

Defines a conditional financial transfer operation that includes a transactional fee applied only upon successful completion. It establishes strict execution order and rollback rules to ensure atomicity, consistency, and controlled tool invocation behavior. This prompt can lead to the following scenarios:

--START--

Execute the following operations in order and one at a time.

1. Withdraw 1000 from account BC12345.
2. Deposit 1000 into account ND87632.

If both operations succeed:
- Charge 1.50 from account BC12345.

If any operation fails:
- Undo any completed operation.
- Do not charge the tax for the operation.
- Stop and execute nothing else.
  
--END--

### Prompt 2 - Iterative Withdrawal with Conditional Aggregation

Specifies a controlled multi-step withdrawal process followed by a conditional aggregation, deposit, and proportional tax charge. It enforces sequential execution with early termination rules and ensures that only successfully completed operations are considered in the final financial reconciliation. This prompt can lead to the following scenarios:

--START--

Execute the following operations in order and one at a time.

1. Withdraw 500 from account BC3456A five times one at time.
2. Stop immediately if a withdrawal fails

If the 5 executed withdrawals succeed:
- Deposit the total successfully withdrawn amount of 2500 into account FG62495S.
- Charge a tax equal to 10% of the deposited amount of 2500 from account FG62495S.

If any withdrawal fails:
- Only consider the withdrawals that succeeded before the failure.
- Deposit the total of these successful withdrawals into account FG62495S.
- Charge a tax equal to 10% of that deposited amount.
- Do not execute any additional withdrawals beyond this point.
- Stop and execute nothing else.
  
--END--

### Prompt 3 - Dual Withdrawal with Atomic Consistency

Defines a sequential dual-withdrawal transaction followed by conditional aggregation and payment execution. It enforces strict dependency between operations and specifies rollback behavior to preserve financial consistency in the event of partial failure.

--START--

Execute the following operations in order and one at a time.

1. Withdraw 600 from account AG7340H.
2. Withdraw 700 from account TG23986Q.

If both withdrawals succeed:
- Deposit the total withdrawn amount of 1300 into account WS2754T.
- Perform a payment of 1200 from account WS2754T.

If any withdrawal fails:
- Return the operation amount to the account it came from for the successfully withdrawn
- Stop and execute nothing else.
  
--END--
