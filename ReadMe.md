# IBIR: Injecting Bugs based on Information Retrieval.

## Description: Partially outdated. Contact ahmed.khanfir@uni.lu for support.

Source-code base of: IBIR and its experimental evaluation.

IBIR injects bugs in locations similar to ones described in a given bug report, using inverted bug-fixing patterns.

It uses the IRFL from iFixR https://github.com/SerVal-DTF/iFixR
and inverted bug-fixing patterns of TBar https://github.com/SerVal-DTF/TBar. The tbar jar was built from this branch (the version used by iFixR): https://github.com/Ahmedfir/JBugInjector/tree/fix_IOBException_VariableReplace_FP 
 

The tool has been evaluated on bugs from defects4j https://github.com/rjust/defects4j.

This approach has been published in https://www.researchgate.net/publication/346973423_IBIR_Bug_Report_driven_Fault_Injection.

## Setup defects4j bugs:

You need java version 8.

On a bash terminal, run:

`./d4j_init.sh`

In the terminal, you should expect this message `Defects4J successfully initialized.` and
you should now have a folder `D4J`, where `defects4j` is installed.

If you want to checkout all the project versions used in the paper's evaluation,
call this by passing the path of your java 8 home as param: 

`./checkoutD4JFixVs_all.sh PATH/TO/YOUR/JAVA8HOME`

Else, you can check-out only your targeted versions. 
i.e. to checkout the fixed version of Lang-50, call:

`./checkoutD4JFixVs_1.sh PATH/TO/YOUR/JAVA8HOME Lang 50`

i.e. with a valid java home path:

`./checkoutD4JFixVs_1.sh /Library/Java/JavaVirtualMachines/jdk1.8.0_212.jdk/Contents/Home/ Lang 50`

If everything goes right, you should see such output in the terminal:

`checking Lang 50`

`Checking out 659ef247 to /PATH/TO/IBIR/D4J/projects/Lang_50 ................OK`

`Init local repository...................................................... OK`

`Tag post-fix revision...................................................... OK`

`Excluding broken/flaky tests............................................... OK`

`Excluding broken/flaky tests............................................... OK`

`Excluding broken/flaky tests............................................... OK`

`Initialize fixed program version........................................... OK`

`Apply patch................................................................ OK`

`Initialize buggy program version........................................... OK`

`Diff 659ef247:b2f1757b..................................................... OK`

`Apply patch................................................................ OK`

`Tag pre-fix revision....................................................... OK`

`Check out program version: Lang-50f........................................ OK`

`Running ant (compile)...................................................... OK`

`Running ant (compile.tests)................................................ OK`
`

You should now have a folder `D4J`, where `defects4j` is installed and 
where all project versions that you have checked-out under `projects` folder. 


## Run the localisation (IRFL) step:

You can find the output of this step under the folder `results/stmtLoc20`.

To reproduce the results of this step, follow the steps in the iFixR repo. 
Make sure that you're applying the localisation on the fixed version and not on the buggy one.

## Run the faults injection step:

You can find the output of this step under the folder `results/ibir_mutation_mat`.

To reproduce the results of this step, you can build the source code of this project 
or use directly the released jar under `release`.

We provide a script as an interface to call the tool: `ibir_inject.sh`.

#### example: generate 2 mutants with bugs semantically similar to the Lang-50's bug-report.

open the terminal and run:

`./ibir_inject.sh 50 2 Lang output PATH/TO/IBIR/stmtLoc/LANG PATH/TO/YOUR/JAVA8HOME`.

i.e. with a valid java home path:

`./ibir_inject.sh 50 2 Lang output /Users/admin/IdeaProjects/IBIR/stmtLoc/LANG /Library/Java/JavaVirtualMachines/jdk1.8.0_212.jdk/Contents/Home/`.

A `logs` folder will be created were you can see allog outputs. An `output` folder will be created, conaining the mutation matrix and the patches of the mutants, as shown in the screenshot below.

![img.png](img.png)



