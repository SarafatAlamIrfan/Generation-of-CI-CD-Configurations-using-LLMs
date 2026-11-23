# Can Free Local LLMs Write CI Configurations?

Can a small, free LLM that runs on a single GPU write a **correct** CI configuration when it is given only a plain-English description of the file?

This repository compares **six open models** on **10 real CI configurations** from public GitHub projects, across three CI services: **Azure Pipelines**, **GitHub Actions** and **Jenkins**. Every generated file is compared with the original and given a correctness verdict.

## What "CI configuration" means here

The pipeline definition files of CI services: Azure Pipelines YAML, GitHub Actions workflows and Jenkinsfiles. These files define how a project is built and tested and, in some projects, also how it is packaged and deployed. Following Ghaleb & Rathnayake (ICSME 2025), the whole file counts as a CI configuration. Three of the ten configurations only build and test; the other seven also deliver or deploy, and results are reported for both groups.

## Background

This work builds on two studies of LLMs for CI configuration generation:

- **T. A. Ghaleb and D. Rathnayake, "Can LLMs Write CI? A Study on Automatic Generation of GitHub Actions Configurations," ICSME 2025 (NIER).** [arXiv:2507.17165](https://arxiv.org/abs/2507.17165) · [replication package](https://github.com/Taher-Ghaleb/ICSME25-LLM4CI). Six LLMs generate GitHub Actions YAML from descriptions in the GitHub documentation, scored by similarity to the reference.
- **T. A. Ghaleb, "Doc2CI: A Multi-Service Study of CI Configuration Generation Using Large Language Models."** [arXiv:2608.01451](https://arxiv.org/abs/2608.01451). Sixteen models on four CI services, scored by similarity and schema validity. Qwen2.5-Coder-14B was the strongest open model, and code specialisation gave no consistent advantage over general models.

This study differs in four ways:

| | Ghaleb & Rathnayake / Doc2CI | This study |
|---|---|---|
| Reference files | snippets from CI documentation | complete pipeline files from real projects, pinned to a commit |
| CI services | GitHub Actions / four services | Azure Pipelines, GitHub Actions, Jenkins (declarative and scripted) |
| Descriptions | documentation text | written from each file and checked claim by claim against it |
| Evaluation | similarity metrics, schema validity | correctness verdict per file, with the reason |

## Method

```
real CI configuration on GitHub  →  description written from it  →  description checked against the file
                                                                              ↓
              verdict + comment  ←  generated file  ←  6 models (same prompt, temperature 0, seed 42)
```

1. **10 real projects**, 5 Azure and 5 Jenkins, that keep their own CI configuration in the repo. The original files are linked (not copied) in [`azure/descriptions.md`](azure/descriptions.md) and [`jenkins/descriptions.md`](jenkins/descriptions.md), pinned to the commit used.
2. **A description of each file**, listing every trigger, agent, job/stage, step, command and value.
3. **Each description checked claim by claim** against its file (Azure: 121 checks, Jenkins: 75 checks, all pass), so a model is never blamed for a vague or wrong description.
4. **Generation.** Each model gets the same system prompt and the description, and never sees the original. Settings for every model and task: `temperature = 0`, `seed = 42`, `num_ctx = 8192`, `num_predict = 2048`. One generation per model per task: 60 outputs.
5. **Evaluation.** Each generated file is compared with the original and given one verdict and a comment saying exactly what differs.

### The 10 configurations

| Platform | # | Project | Type | Scope |
|---|---|---|---|---|
| Azure | 1 | Azure Pipeline Basic (Python + pytest) | Azure Pipelines | Build & test |
| Azure | 2 | JS E2E Express Server (Node.js + JUnit) | Azure Pipelines | Build & test |
| Azure | 3 | Azure Batch Samples (Python, Java, .NET) | Azure Pipelines | Build & test |
| Azure | 4 | Todo App on Azure (azd deploy) | GitHub Actions | Build & test + deploy |
| Azure | 5 | Java EE 7 Petstore to Azure Web App | GitHub Actions | Build & test + deploy |
| Jenkins | 1 | Simple Java Maven App | Declarative | Build & test + deliver |
| Jenkins | 2 | Simple Python PyInstaller App | Declarative | Build & test + deliver |
| Jenkins | 3 | Multibranch Pipeline (Node.js / React) | Declarative | Build & test + deploy |
| Jenkins | 4 | DevOps Spring Boot (Maven + Docker Hub) | Declarative | Build & test + deliver |
| Jenkins | 5 | Spring Boot Docker Deploy | Scripted | Build & test + deploy |

### The 6 models

All six are free, run locally with [Ollama](https://ollama.com), and fit on one 16 GB GPU.

| Model | Kind | Why it is included |
|---|---|---|
| `qwen2.5-coder:7b` | code | among the strongest open models for CI generation in Doc2CI |
| `qwen2.5-coder:14b` | code | same family at twice the size; the best open model in Doc2CI |
| `deepseek-coder-v2:16b` | code (Mixture-of-Experts) | a different model family, not yet evaluated on CI configuration generation |
| `gemma3:12b` | general | evaluated by Ghaleb & Rathnayake |
| `llama3.1:8b` | general | evaluated by Ghaleb & Rathnayake |
| `codegemma:7b` | code | evaluated by Ghaleb & Rathnayake |

Together: 4 code-specialised and 2 general-purpose models from 4 families (Qwen, DeepSeek, Google, Meta), 7B to 16B parameters.

### Verdicts

| Verdict | Meaning |
|---|---|
| **Correct** | matches the original; any differences don't change what the pipeline does |
| **Mostly correct** | works, with only small differences such as a display name or an extra harmless option |
| **Partially correct** | the structure is right, but something is missing or wrong, so part of the pipeline misbehaves |
| **Not correct** | would fail to load or fail when run, or produced no file |

The same rules apply to every model:
- a pipeline the CI service would refuse to load (invalid syntax, an option placed as a step) → **Not correct**;
- a wrong path or input name that makes a job fail → **Not correct**;
- printing any part of a secret to the logs → no better than **Partially correct**;
- anything added that the description didn't ask for (such as a branch filter) is a difference, even if it looks sensible.

## Results

### Azure

| # | Project | qwen 7B | qwen 14B | deepseek 16B | Gemma 3 12B | Llama 3.1 8B | CodeGemma 7B |
|---|---|---|---|---|---|---|---|
| 1 | Azure Pipeline Basic | Correct | Correct | Correct | Not correct | Not correct | Correct |
| 2 | JS E2E Express Server | Partially correct | Mostly correct | Correct | Not correct | Not correct | Partially correct |
| 3 | Azure Batch Samples | Not correct | Correct | Not correct | Not correct | Not correct | Not correct |
| 4 | Todo App (azd deploy) | Not correct | Partially correct | Not correct | Not correct | Not correct | Not correct |
| 5 | Petstore to Azure Web App | Partially correct | Correct | Mostly correct | Mostly correct | Mostly correct | Correct |

### Jenkins

| # | Project | qwen 7B | qwen 14B | deepseek 16B | Gemma 3 12B | Llama 3.1 8B | CodeGemma 7B |
|---|---|---|---|---|---|---|---|
| 1 | Simple Java Maven App | Correct | Correct | Not correct | Partially correct | Not correct | Not correct |
| 2 | Simple Python PyInstaller App | Partially correct | Mostly correct | Partially correct | Correct | Correct | Not correct |
| 3 | Multibranch Pipeline | Not correct | Correct | Correct | Correct | Not correct | Not correct |
| 4 | DevOps Spring Boot | Not correct | Correct | Partially correct | Not correct | Not correct | Not correct |
| 5 | Spring Boot Docker Deploy (scripted) | Partially correct | Partially correct | Not correct | Partially correct | Partially correct | Not correct |

### Totals and ranking (10 configurations)

Score = 3 × Correct + 2 × Mostly correct + 1 × Partially correct (maximum 30).

| Rank | Model | Correct | Mostly | Partially | Not correct | Score |
|---|---|---|---|---|---|---|
| 1 | qwen2.5-coder:14b | 6 | 2 | 2 | 0 | 24 |
| 2 | deepseek-coder-v2:16b | 3 | 1 | 2 | 4 | 13 |
| 3 | qwen2.5-coder:7b | 2 | 0 | 4 | 4 | 10 |
| 3 | gemma3:12b | 2 | 1 | 2 | 5 | 10 |
| 5 | codegemma:7b | 2 | 0 | 1 | 7 | 7 |
| 6 | llama3.1:8b | 1 | 1 | 1 | 7 | 6 |

### By scope

| | qwen 7B | qwen 14B | deepseek 16B | Gemma 3 12B | Llama 3.1 8B | CodeGemma 7B |
|---|---|---|---|---|---|---|
| Build & test (3): Correct or Mostly | 1 | 3 | 2 | 0 | 0 | 1 |
| + deliver / deploy (7): Correct or Mostly | 1 | 5 | 2 | 3 | 2 | 1 |

Per-file comments are in [`results/azure_results.csv`](results/azure_results.csv) and [`results/jenkins_results.csv`](results/jenkins_results.csv). [`results/evaluation.xlsx`](results/evaluation.xlsx) has every description, generated file, verdict and comment, a summary for all six models, and the ranking.

## Findings

1. **qwen2.5-coder:14b is clearly the most reliable.** 8 of 10 configurations are correct or mostly correct, and it is the only model with no "Not correct". This agrees with Doc2CI, where it was the strongest open model.
2. **Size helps within a family.** qwen2.5-coder goes from 2 to 6 correct configurations between 7B and 14B.
3. **Code specialisation alone does not help.** codegemma:7b scored below the general-purpose gemma3:12b, and qwen2.5-coder:7b was weak. This matches both earlier studies.
4. **"Valid" is not "correct".** All 30 qwen and deepseek outputs pass a YAML parse or a Jenkinsfile structure check, yet 8 would fail. codegemma:7b passes all 10 checks but gets 7 "Not correct".
5. **Jenkins failures are mostly pipelines that never start**, because of invented syntax: `agent any { ... }`, `skipStagesAfterUnstable()` outside `options`, invalid `scmGit(...)` checkouts, or a declarative pipeline where a scripted one was asked for.
6. **Authentication and deployment are hardest.** No model got the azd deployment with federated and client-credential logins (Azure #4) right. qwen2.5-coder:14b came closest but prints part of the client secret to the logs; the others dropped login arguments, used flags that don't exist or mishandled the secret. In the scripted Jenkinsfile (Jenkins #5), every model broke the push or deploy steps.
7. **Small wrong details break whole jobs.** Wrong task input names (`version` for `versionSpec`, `pomFile` or `mavenPom` for `mavenPomFile`) silently drop values: in Azure #3, four of six models lost or broke the Maven POM path, so the Java job would fail.
8. **Repetition loops.** At temperature 0, gemma3:12b produced no file for Azure #1 (it kept repeating itself until Ollama stopped it), and llama3.1:8b repeated `displayName: ''` in Azure #2 until the token limit.

## Run details

| | qwen2.5-coder 7B / 14B, deepseek-coder-v2 16B | gemma3 12B, llama3.1 8B, codegemma 7B |
|---|---|---|
| Notebooks | [`azure/notebook/`](azure/notebook), [`jenkins/notebook/`](jenkins/notebook) | [`notebook/generate_free_models_kaggle.ipynb`](notebook/generate_free_models_kaggle.ipynb) |
| Platform | Kaggle, Ollama | Kaggle, Tesla T4 (15 GB), Ollama 0.40.17 |
| Settings | temperature 0, seed 42, num_ctx 8192, num_predict 2048 | same |
| Seconds per file | not recorded | gemma3 21.3 · llama3.1 19.3 · codegemma 12.2 |

## Limitations

- **Small sample:** 10 configurations, one generation per model, so no statistical tests.
- **Not executed:** verdicts come from comparing each file with the original, not from running the pipelines.
- **Public sources:** the original files are public, so the models may have seen them during training.
- **Reproducibility:** temperature 0 and a fixed seed make re-runs very close, but Ollama doesn't guarantee identical output across GPUs or versions, which is why every generated file is kept here.

## Repository layout

```
azure/
  descriptions.md                 the 5 descriptions, each linked to its original file and commit
  notebook/                       Kaggle notebook: qwen2.5-coder 7B/14B and deepseek-coder-v2 16B
  generated/<model>/              the file each model produced, for all 6 models
jenkins/
  descriptions.md
  notebook/
  generated/<model>/
notebook/
  generate_free_models_kaggle.ipynb   Kaggle notebook: gemma3 12B, llama3.1 8B and codegemma 7B (both platforms)
results/
  evaluation.xlsx                 Summary (all 6 models), All 6 models (counts, scope split, ranking), Azure, Jenkins
  azure_results.csv               verdict and comment per model and project
  jenkins_results.csv
```

`azure/generated/gemma3-12b/1_python_pytest_azp.NO_OUTPUT.md` records the one task where a model produced no file.

## Running it yourself

1. On **Kaggle**: File → Import Notebook, and choose one of the notebooks.
2. Settings: **Accelerator: GPU T4 x2**, **Internet: On**.
3. **Save Version → Save & Run All (Commit)**. The notebook installs Ollama, pulls the models and writes every output to `/kaggle/working` (about 15–25 minutes).

## Credits

The original CI configurations belong to their authors and are **not copied** into this repository; the description files link to each one, pinned to the commit used:
[atulkamble/azure-pipeline-basic](https://github.com/atulkamble/azure-pipeline-basic),
[Azure-Samples/js-e2e-express-server](https://github.com/Azure-Samples/js-e2e-express-server),
[Azure-Samples/azure-batch-samples](https://github.com/Azure-Samples/azure-batch-samples),
[Azure-Samples/todo-nodejs-mongo](https://github.com/Azure-Samples/todo-nodejs-mongo),
[Azure-Samples/Java-application-petstore-ee7](https://github.com/Azure-Samples/Java-application-petstore-ee7),
[jenkins-docs/simple-java-maven-app](https://github.com/jenkins-docs/simple-java-maven-app),
[jenkins-docs/simple-python-pyinstaller-app](https://github.com/jenkins-docs/simple-python-pyinstaller-app),
[jenkins-docs/building-a-multibranch-pipeline-project](https://github.com/jenkins-docs/building-a-multibranch-pipeline-project),
[saravanan81java/devops-springboot](https://github.com/saravanan81java/devops-springboot),
[vdharmaraj/PGDO_Proj3](https://github.com/vdharmaraj/PGDO_Proj3).

Model references: Qwen2.5-Coder ([arXiv:2409.12186](https://arxiv.org/abs/2409.12186)), DeepSeek-Coder-V2 ([arXiv:2406.11931](https://arxiv.org/abs/2406.11931)), Gemma 3, Llama 3.1 and CodeGemma via the [Ollama library](https://ollama.com/library).

---
Md Sarafat Alam Mozumder · East West University
