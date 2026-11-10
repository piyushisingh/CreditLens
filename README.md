# CreditLens

CreditLens is an experimental research prototype and decision-support mobile application currently under active development. The project explores the integration of credit risk machine learning models with a native Android client (Kotlin & Jetpack Compose), translating an approved Lovable UI prototype into native mobile components.

> [!NOTE]
> **Project Status & Technical Disclaimers:**
> - **Research Prototype:** This project is an ongoing research and engineering prototype under active development.
> - **No Production or Regulatory Claims:** CreditLens does not claim production readiness, validated real-world deployment performance, or formal regulatory compliance (such as Basel II/III or IFRS 9 certification).
> - **Recovery Rate Assumption:** The Loss Given Default parameter ($\text{LGD} = 0.90$) reflects an initial baseline research modeling assumption ($\text{LGD} = 1 - \text{recovery\_rate}$ assuming an exploratory 10% recovery rate), not an empirical or universal industry standard. In commercial lending, recovery rates vary substantially depending on facility type, collateral quality, liquidation costs, and macroeconomic conditions.

---

## 🏛️ Project Architecture

The CreditLens repository is structured into distinct modules:

```
CreditLens/
├── backend/        # Python-based credit risk inference service & API (under development)
├── android/        # Native Android application (Kotlin + Jetpack Compose)
├── docs/           # Architecture diagrams, API specifications, and design references
├── .gitignore      # Multi-stack ignore rules (Python, Android, Gradle, IDE, datasets)
└── README.md       # Project overview, prototype scope, and technical documentation
```

---

## 🔬 Modeling Framework & Risk Formulation

The underlying risk methodology models Expected Credit Loss (ECL) using the tri-factor formulation:

$$\text{Expected Loss} = \text{PD} \times \text{LGD} \times \text{EAD}$$

* **Probability of Default (PD):** Predicted through classification models trained on borrower risk characteristics.
* **Loss Given Default (LGD):** Parametric baseline set at $0.90$ (derived from a hypothetical 10% recovery rate assumption).
* **Exposure at Default (EAD):** Outstanding loan balance (`loan_amt_outstanding`).

### Input Features
* `credit_lines_outstanding`: Number of existing active credit facilities.
* `loan_amt_outstanding`: Total balance owed on the current loan.
* `total_debt_outstanding`: Cumulative outstanding debt obligations.
* `income`: Annual borrower income.
* `years_employed`: Length of steady employment.
* `fico_score`: Credit bureau score.

### Derived Risk Ratios
* $\text{debt\_to\_income} = \frac{\text{total\_debt\_outstanding}}{\text{income}}$
* $\text{loan\_to\_income} = \frac{\text{loan\_amt\_outstanding}}{\text{income}}$
* $\text{debt\_per\_credit\_line} = \frac{\text{total\_debt\_outstanding}}{\text{credit\_lines\_outstanding} + 1}$

---

## 🛠️ Technology Stack

* **Mobile App:** Kotlin, Jetpack Compose, Material 3, Android Architecture Components
* **Backend Service:** Python, FastAPI, scikit-learn, XGBoost, Pydantic
* **Repository:** [https://github.com/piyushisingh/CreditLens](https://github.com/piyushisingh/CreditLens)

---

## 🗺️ Project Milestones & Roadmap

- [x] **Milestone 1:** Workspace isolation, target Git setup, and clean architecture scaffolding.
- [ ] **Milestone 2:** Backend service setup, feature transformation pipeline, and model artifact loading.
- [ ] **Milestone 3:** Native Android project initialization, dependency injection, and theme/design tokens.
- [ ] **Milestone 4:** Jetpack Compose UI implementation (rebuilding the approved Lovable prototype).
- [ ] **Milestone 5:** Mobile-to-backend networking, end-to-end integration, and scenario testing.
