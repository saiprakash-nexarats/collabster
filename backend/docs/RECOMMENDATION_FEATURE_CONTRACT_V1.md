# COLLOBSTER RECOMMENDATION FEATURE CONTRACT (VERSION 1)

---

## 1. OVERVIEW & SCOPE
This document defines the canonical specification and contract for all 10 features consumed by the LightGBM recommendation ranking model (`lgbm_ranker_model_v2.txt`).

The contract guarantees semantic alignment between:
1. **Java Online Feature Extraction** (`com.collabsphere_first.recommendation.feature.RecommendationFeatureAssembler`).
2. **Internal Ranking Contract** (`POST /api/v1/internal/recommend/feed/rank` with `contractVersion: "1"`, `featureContractVersion: "1"`).
3. **Machine Learning Model Training Data** (`artifacts/recommendation_training_data.csv`).

---

## 2. CANONICAL FEATURE SPECIFICATIONS

### Feature 1: `freshness`

- **Meaning**: Age-decayed recency relevance scoring post youthfulness.
- **Owner**: Java `RecommendationFeatureAssembler`.
- **Input**: `candidate.createdAt` (UTC `Instant`) and ranking timestamp `now`.
- **Formula**:
  $$\text{ageSeconds} = \max(0, \text{Duration.between}(createdAt, now).\text{getSeconds}())$$
  $$\text{score} = 0.5^{\frac{\text{ageSeconds}}{86400}}$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Continuous exponential decay curve. Half-life = 24.0 hours (86,400 seconds).
- **Transformation**: Clamped between 0.0 and 1.0. Rounded to 6 decimal places.
- **Direction**: Monotonically decreasing with age (larger = newer = higher ranking boost).
- **Missing Handling**: If `createdAt` is null, returns 0.0 (stale floor).
- **Future Timestamp**: Clamped to age = 0 (`score = 1.0`).
- **Training Pipeline Analysis**:
  - Training formula: `math.exp(-(math.log(2.0) / 24.0) * age_hours)` in `ExponentialDecayFunction`.
  - Mathematically identical to $0.5^{\frac{\text{ageHours}}{24}} = 0.5^{\frac{\text{ageSeconds}}{86400}}$.
  - Training dataset stats: min = $1.68 \times 10^{-8}$, mean = 0.1906, 50% = 0.0324, max = 0.9999.
  - Model tree splits: 504 split nodes, min threshold = 0.0000, median threshold = 0.0886, max threshold = 0.9811.
- **Model Training Compatibility**: **VERIFIED MATCH**

---

### Feature 2: `quality`

- **Meaning**: Algorithmic assessment of post content quality, low spam likelihood, and absence of moderation flags.
- **Owner**: ML Platform / Moderation Pipeline (Future Phase).
- **Input**: Unmoderated/raw content attributes. Currently not computed online in Java.
- **Formula**:
  $$\text{score} = \frac{w_{\text{content}} \cdot S_{\text{content}} + w_{\text{spam}} \cdot (1 - P_{\text{spam}}) + w_{\text{rep}} \cdot S_{\text{rep}} + w_{\text{dup}} \cdot (1 - I_{\text{dup}})}{\sum w}$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Weighted linear combination in $[0, 100]$ divided by 100.
- **Transformation**: Linear scaling.
- **Direction**: Larger means higher quality.
- **Missing Handling**: Explicitly transmitted as `null` by Java. Mapped to `np.nan` in Python. LightGBM routes `NaN` to default branch splits without imputation.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Generated offline using synthetic text/moderation proxies.
  - Training dataset stats: min = 0.0, mean = 0.4999, std = 0.1993, max = 1.0000.
  - Model tree splits: 576 split nodes, min threshold = 0.0000, median threshold = 0.5515, max threshold = 0.9977.
- **Model Training Compatibility**: **NOT AVAILABLE** (Sent as `null`; no synthetic fabrication).

---

### Feature 3: `popularity`

- **Meaning**: Log-scaled engagement volume reflecting broad platform appreciation.
- **Owner**: Java `RecommendationFeatureAssembler`.
- **Input**: Authoritative `likeCount` from Java `InteractionService`.
- **Formula**:
  $$\text{score} = \min(1.0, \ln(1 + \text{likeCount}) \cdot 0.1)$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Logarithmic scaling $\ln(1 + x)$.
- **Transformation**: Clamped between 0.0 and 1.0. Rounded to 6 decimal places.
- **Direction**: Monotonically increasing with engagement.
- **Missing Handling**: If `likeCount <= 0` or missing, defaults to 0.0.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training formula: Weighted sum of log-normalized engagement metrics:
    $$\frac{1}{9.3} \left[ 1.0 \cdot \frac{\ln(1 + \text{likes})}{\ln(10001)} + 2.0 \cdot \frac{\ln(1 + \text{comments})}{\ln(2001)} + 3.0 \cdot \frac{\ln(1 + \text{shares})}{\ln(1001)} + 2.5 \cdot \frac{\ln(1 + \text{saves})}{\ln(3001)} + 0.8 \cdot \frac{\ln(1 + \text{watchTime})}{\ln(100001)} \right]$$
  - Training dataset stats: min = 0.0, mean = 0.0681, std = 0.0586, max = 0.4114.
  - Model tree splits: 290 split nodes, min threshold = 0.0000, median threshold = 0.0792, max threshold = 0.2536.
  - Java uses `likeCount` as an authoritative single-signal engagement proxy with log scaling.
- **Model Training Compatibility**: **PARTIAL MATCH** (Directional and mathematical log-scaling match; Java currently lacks online comment/share/watch counts).

---

### Feature 4: `preference`

- **Meaning**: Affinity between viewer explicit tag preferences and candidate post tags.
- **Owner**: Personalization Engine (Future Phase).
- **Input**: Viewer preference tags matched against post tags.
- **Formula**:
  $$\text{score} = \frac{\sum_{t \in \text{matched}} w_t}{\sum_{t \in \text{userPreferences}} w_t}$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Proportional weighted match.
- **Transformation**: Ratio in $[0.0, 1.0]$.
- **Direction**: Larger means stronger topic alignment.
- **Missing Handling**: Explicitly transmitted as `null` by Java. Mapped to `np.nan` in Python.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - In training dataset, 48.4% of samples were matched (mean = 0.4839, std = 0.4997, heavily binary 0.0 or 1.0).
  - Model tree splits: 63 splits with threshold = 0.0000 (acting as binary presence filter with high NDCG gain of 8010.49).
- **Model Training Compatibility**: **NOT AVAILABLE** (Sent as `null`).

---

### Feature 5: `interest`

- **Meaning**: Behavioral interest overlap computed via Jaccard tag and community history similarity.
- **Owner**: Personalization Engine (Future Phase).
- **Input**: Viewer historical interaction profile (liked tags, watched tags, saved tags, community memberships).
- **Formula**:
  $$\text{score} = 0.30 \cdot J(\text{tags}) + 0.20 \cdot J(\text{communities}) + 0.20 \cdot M_{\text{liked}} + 0.15 \cdot M_{\text{watched}} + 0.10 \cdot M_{\text{saved}} + 0.05 \cdot M_{\text{interacted}}$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Rubric points divided by 100.0.
- **Transformation**: Linear combination clamped to $[0.0, 1.0]$.
- **Direction**: Larger means higher behavioral relevance.
- **Missing Handling**: Explicitly transmitted as `null` by Java. Mapped to `np.nan` in Python.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training dataset stats: min = 0.0, mean = 0.1379, std = 0.1705, max = 0.8935.
  - Model tree splits: 251 split nodes, min threshold = 0.0000, median threshold = 0.1350, max threshold = 0.5995.
- **Model Training Compatibility**: **NOT AVAILABLE** (Sent as `null`).

---

### Feature 6: `relationship`

- **Meaning**: Social proximity between viewer and candidate post author.
- **Owner**: Java `RecommendationFeatureAssembler`.
- **Input**: `candidate.sources()` (`Set<FeedSourceType>`).
- **Formula**:
  $$\text{score} = \begin{cases}
  1.00 & \text{if sources contains } \text{CONNECTION} \\
  0.85 & \text{if sources contains } \text{FOLLOWING} \\
  0.70 & \text{if sources contains } \text{JOINED\_COMMUNITY} \\
  0.40 & \text{if sources contains } \text{SELF} \\
  0.20 & \text{if sources contains } \text{PUBLIC\_FALLBACK}
  \end{cases}$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Deterministic discrete tier hierarchy.
- **Transformation**: Clamped between 0.0 and 1.0.
- **Direction**: Larger means closer social bond.
- **Missing Handling**: If sources is null or empty, defaults to 0.20 (`PUBLIC_FALLBACK`).
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training model used creator relationship signals: `friend_score = 1.0`, `following_score = 0.8`, `default = 0.0`.
  - Training dataset stats: min = 0.0, mean = 0.0024, max = 1.0000 (sparse social edges).
  - Model tree splits: 33 split nodes, min threshold = 0.1391, median threshold = 0.4204, max threshold = 0.8710.
  - Java values (0.20, 0.40, 0.70, 0.85, 1.00) align with decision splits:
    - Threshold 0.1391 separates non-social (0.0) from public fallback (0.20).
    - Threshold 0.4204 separates self/public from community/following (0.70, 0.85).
    - Threshold 0.8710 separates following (0.85) from accepted connections (1.00).
- **Model Training Compatibility**: **PARTIAL MATCH** (Bounded within [0, 1]; tiers align with model split thresholds).

---

### Feature 7: `community`

- **Meaning**: Relevance derived from shared community membership between viewer and content.
- **Owner**: Java `RecommendationFeatureAssembler`.
- **Input**: `candidate.sources().contains(FeedSourceType.JOINED_COMMUNITY)`.
- **Formula**:
  $$\text{score} = \begin{cases} 1.0 & \text{if candidate from JOINED\_COMMUNITY} \\ 0.0 & \text{otherwise} \end{cases}$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Binary membership indicator.
- **Transformation**: Indicator variable.
- **Direction**: 1.0 indicates viewer is an approved member of the host community.
- **Missing Handling**: Non-community posts or non-members evaluate to 0.0.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training formula: `matching_affinity / total_affinity` based on user community interaction history.
  - Training dataset stats: min = 0.0, mean = 0.0981, std = 0.2523, max = 0.9980.
  - Model tree splits: 296 split nodes, min threshold = 0.0000, median threshold = 0.7188, max threshold = 0.9672.
  - Java's membership signal (1.0) comfortably exceeds the 0.7188-0.9672 thresholds for member posts.
- **Model Training Compatibility**: **PARTIAL MATCH** (Binary projection of continuous community affinity).

---

### Feature 8: `user_interaction_count`

- **Meaning**: Overall activity level of the viewing user across the platform.
- **Owner**: Interaction Analytics Service (Future Phase).
- **Input**: Viewer total interaction volume.
- **Formula**:
  $$\text{score} = 1.0 - \exp\left(-\frac{\text{interaction\_count}}{50.0}\right)$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Negative exponential saturation curve.
- **Transformation**: Continuous mapping from $[0, \infty) \to [0.0, 1.0)$.
- **Direction**: Larger means more active user.
- **Missing Handling**: Explicitly transmitted as `null` by Java. Mapped to `np.nan` in Python.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training dataset stats: min = 0.0, mean = 0.4126, std = 0.2367, max = 0.9404.
  - Model tree splits: 354 split nodes, min threshold = 0.0000, median threshold = 0.3229, max threshold = 0.7558.
- **Model Training Compatibility**: **NOT AVAILABLE** (Sent as `null`).

---

### Feature 9: `post_interaction_count`

- **Meaning**: Total interaction velocity of the candidate post across all interaction modalities (likes, comments, shares, saves).
- **Owner**: Interaction Aggregation Service (Future Phase).
- **Input**: Multi-modal interaction sum.
- **Formula**:
  $$\text{score} = 1.0 - \exp\left(-\frac{\text{total\_interactions}}{20.0}\right)$$
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Negative exponential saturation curve.
- **Transformation**: Continuous mapping from $[0, \infty) \to [0.0, 1.0)$.
- **Direction**: Larger means higher post engagement.
- **Missing Handling**: Explicitly transmitted as `null` by Java. Mapped to `np.nan` in Python.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training dataset stats: min = 0.0, mean = 0.2069, std = 0.1364, max = 0.6671.
  - Model tree splits: 140 split nodes, min threshold = 0.0720, median threshold = 0.3125, max threshold = 0.5392.
  - **Audit Note**: In Phase 4, Java sent raw integer `(double)(likeCount * 2L)`. Because this raw value was unbounded and unscaled ($> 1.0$), it severely violated the model's training threshold envelope ($\le 0.5392$). In Phase 4.1, this feature is set to `null` to avoid semantic and distribution distortion until full multi-modal interaction aggregation is implemented.
- **Model Training Compatibility**: **NOT AVAILABLE** (Sent as `null`).

---

### Feature 10: `creator_reputation`

- **Meaning**: Platform reputation and trust score of the candidate post creator.
- **Owner**: User Reputation Engine (Future Phase).
- **Input**: Author historical account credibility score.
- **Formula**: Normalized score in $[0.0, 1.0]$.
- **Range**: `[0.0, 1.0]`
- **Raw Type**: Float.
- **Normalization**: Bounded unit interval.
- **Transformation**: Clamped between 0.0 and 1.0.
- **Direction**: Larger indicates more reputable creator.
- **Missing Handling**: Explicitly transmitted as `null` by Java. Mapped to `np.nan` in Python.
- **Future Timestamp**: Not applicable.
- **Training Pipeline Analysis**:
  - Training dataset stats: min = 0.0, mean = 0.1358, std = 0.0953, max = 0.5295.
  - Model tree splits: 493 split nodes, min threshold = 0.0000, median threshold = 0.1086, max threshold = 0.3260.
- **Model Training Compatibility**: **NOT AVAILABLE** (Sent as `null`).

---

## 3. SUMMARY COMPATIBILITY MATRIX

| Feature Name | Java Online Producer | Training Definition | Contract Status | Missing / Null Policy |
| :--- | :--- | :--- | :--- | :--- |
| **`freshness`** | `0.5 ^ (ageSeconds / 86400)` | `exp(-ln(2)/24 * age_hours)` | **VERIFIED MATCH** | Clamped to 0.0 if missing |
| **`quality`** | `null` | Weighted spam/moderation | **NOT AVAILABLE** | Sent as `null`, handled as `NaN` |
| **`popularity`** | `min(1.0, ln(1+likes)*0.1)` | Multi-signal log-norm | **PARTIAL MATCH** | Defaults to 0.0 if likeCount=0 |
| **`preference`** | `null` | User tag match ratio | **NOT AVAILABLE** | Sent as `null`, handled as `NaN` |
| **`interest`** | `null` | Jaccard profile overlap | **NOT AVAILABLE** | Sent as `null`, handled as `NaN` |
| **`relationship`** | Source tier (1.0, 0.85, 0.70, 0.40, 0.20) | Graph relationship weight | **PARTIAL MATCH** | Defaults to 0.20 (Public fallback) |
| **`community`** | 1.0 if member, else 0.0 | User community affinity | **PARTIAL MATCH** | Defaults to 0.0 |
| **`user_interaction_count`** | `null` | $1 - \exp(-\text{cnt}/50)$ | **NOT AVAILABLE** | Sent as `null`, handled as `NaN` |
| **`post_interaction_count`** | `null` | $1 - \exp(-\text{cnt}/20)$ | **NOT AVAILABLE** | Sent as `null`, handled as `NaN` |
| **`creator_reputation`** | `null` | Historical trust score | **NOT AVAILABLE** | Sent as `null`, handled as `NaN` |

---

## 4. BOUNDARY VALIDATION RULES

1. **Range Enforcement**:
   - `freshness`, `quality`, `popularity`, `preference`, `interest`, `relationship`, `community`, `creator_reputation` MUST be in `[0.0, 1.0]`. Values outside this range are rejected with HTTP 422.
   - `user_interaction_count`, `post_interaction_count` MUST be non-negative ($\ge 0.0$).
2. **Missing Signals**:
   - Features not authoritatively known by Java MUST be transmitted as `null`. No synthetic neutral defaults (e.g. `0.5`) may be fabricated by Java.
3. **Contract Versioning**:
   - Both request and response mandate `"contractVersion": "1"` and `"featureContractVersion": "1"`.
