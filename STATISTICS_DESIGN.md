# Advanced Statistics & Analytics Design

## Overview
This document outlines the design for an advanced statistics and analytics system similar to Tinkoff Investments, providing deep insights into business performance with visual charts and detailed breakdowns.

## Screen Structure

### 1. Statistics Dashboard (Main Screen)
**Location:** `StatsScreen.kt` (enhanced)

**Purpose:** Overview with key metrics and quick access to detailed analytics

**Layout:**
- Period selector (Day/Month/Year) - top
- Date/Period picker - below selector
- Key metrics cards (clickable) - main area
- Quick insights section - bottom

**Metrics Cards (Clickable):**
1. **Total Income** → Opens Income Details Screen
   - Large number: Total income for period
   - Subtitle: "Средний чек: X ₽" (Average check)
   - Trend indicator: ↑/↓ vs previous period
   
2. **Total Visits** → Opens Visits Analytics Screen
   - Large number: Total appointments
   - Subtitle: "Среднее в день: X" (Average per day)
   - Trend indicator
   
3. **Total Clients** → Opens Clients Analytics Screen
   - Large number: Unique clients
   - Subtitle: "Новых: X" (New clients count)
   - Trend indicator
   
4. **Profit** (Income - Expenses)
   - Large number: Net profit
   - Subtitle: "Маржа: X%" (Profit margin)
   - Color: Green if positive, Red if negative

**Quick Insights Section:**
- Best day by income (clickable → Income Details)
- Best client by income (clickable → Clients Analytics)
- Most frequent client (clickable → Clients Analytics)
- Most loaded day of week (text only)

---

### 2. Income Details Screen
**Route:** `stats/income/{period}/{dateKey}`

**Purpose:** Deep dive into income analytics

**Sections:**

#### A. Summary Cards
- **Total Income** - large number
- **Average Income per Day** - calculated
- **Average Check** - income / visits
- **Income Growth** - % vs previous period

#### B. Income Over Time Chart
- **Type:** Line chart
- **X-axis:** Days (for month/year) or Hours (for day)
- **Y-axis:** Income in rubles
- **Features:**
  - Interactive: tap on point shows exact value
  - Smooth curve
  - Highlight current period vs previous period (dashed line)

#### C. Income by Day of Week
- **Type:** Bar chart
- **X-axis:** Monday, Tuesday, ..., Sunday
- **Y-axis:** Total income per day
- **Features:**
  - Color intensity based on value
  - Tap to see exact amount

#### D. Top Days by Income
- **Type:** List with cards
- Shows top 5-10 days
- Each card: Date, Income, % of total
- Clickable → Day detail screen

---

### 3. Clients Analytics Screen
**Route:** `stats/clients/{period}/{dateKey}`

**Purpose:** Client contribution and behavior analysis

**Sections:**

#### A. Summary Cards
- **Total Clients** - unique count
- **New Clients** - first visit in period
- **Returning Clients** - repeat visits
- **Average Income per Client**

#### B. Client Contribution Chart
- **Type:** Donut/Pie chart
- **Shows:** Each client's % of total income
- **Features:**
  - Tap segment to highlight client
  - Show client name and exact amount
  - Limit to top 10 clients, rest as "Others"
  - Color-coded segments

#### C. Top Clients List
- **Type:** Scrollable list
- **Columns:**
  - Client name
  - Total income
  - % of total income
  - Number of visits
  - Average check
- **Features:**
  - Sortable by income/visits
  - Clickable → Client detail screen
  - Shows top 20 clients

#### D. Client Distribution
- **Type:** Bar chart
- **Shows:** Income distribution by client segments
  - Top 3 clients
  - Next 7 clients
  - Rest of clients
- **Purpose:** Visualize income concentration

---

### 4. Visits Analytics Screen
**Route:** `stats/visits/{period}/{dateKey}`

**Purpose:** Appointment patterns and frequency analysis

**Sections:**

#### A. Summary Cards
- **Total Visits** - appointment count
- **Average Visits per Day**
- **Most Active Day** - day with most visits
- **Average Visit Duration** - in minutes

#### B. Visits Over Time
- **Type:** Line chart
- **X-axis:** Days/Hours
- **Y-axis:** Number of visits
- **Features:**
  - Compare with previous period
  - Show trend line

#### C. Visits by Day of Week
- **Type:** Bar chart
- **X-axis:** Days of week
- **Y-axis:** Visit count
- **Features:**
  - Identify busiest days
  - Color-coded by intensity

#### D. Visit Frequency Distribution
- **Type:** Histogram
- **Shows:** How many clients visited X times
- **Example:**
  - 1 visit: 10 clients
  - 2-3 visits: 5 clients
  - 4+ visits: 2 clients

---

### 5. Reports & Insights Screen
**Route:** `stats/insights/{period}/{dateKey}`

**Purpose:** AI-like insights and recommendations

**Sections:**

#### A. Key Insights (Auto-generated)
1. **Best Day by Income**
   - Date, Income amount
   - Comparison with average
   
2. **Best Month by Income** (if period is Year)
   - Month name, Income amount
   - Growth percentage
   
3. **Client Who Paid Most**
   - Client name, Total income
   - Number of visits
   - Clickable → Client detail
   
4. **Most Frequent Client**
   - Client name, Visit count
   - Total income from this client
   - Clickable → Client detail
   
5. **Most Loaded Day of Week**
   - Day name, Average income
   - Number of visits
   
6. **Least Loaded Day of Week**
   - Day name, Average income
   - Suggestion: "Consider promotions"
   
7. **Peak Hours** (if period is Day/Month)
   - Time range, Visit count
   - Income during peak
   
8. **Revenue Trends**
   - "Income increased by X% vs previous period"
   - "Best performing: [Day/Month]"
   - "Growth opportunity: [Day/Month]"

#### B. Recommendations
- "Consider increasing availability on [Day]"
- "Top client [Name] brings X% of income - maintain relationship"
- "Average check increased by X% - great job!"

---

## Data Models & Aggregation Logic

### New Data Classes

```kotlin
// Client Statistics
data class ClientStats(
    val clientId: Long,
    val clientName: String,
    val totalIncome: Long, // in cents
    val visitCount: Int,
    val averageCheck: Long, // in cents
    val incomePercentage: Float, // % of total income
    val firstVisitDate: LocalDate?,
    val lastVisitDate: LocalDate?
)

// Day Statistics
data class DayStats(
    val date: LocalDate,
    val dateKey: String,
    val income: Long,
    val expenses: Long,
    val profit: Long,
    val visitCount: Int,
    val clientCount: Int,
    val dayOfWeek: Int // 1-7 (Monday-Sunday)
)

// Period Comparison
data class PeriodComparison(
    val currentIncome: Long,
    val previousIncome: Long,
    val growthPercentage: Float,
    val growthAmount: Long
)

// Visit Frequency Distribution
data class VisitFrequencyBucket(
    val visitCount: Int, // e.g., 1, 2-3, 4-5, 6+
    val clientCount: Int
)
```

### Repository Methods (New)

```kotlin
// Client Analytics
suspend fun getClientStats(startDate: String, endDate: String): List<ClientStats>
suspend fun getTopClientsByIncome(startDate: String, endDate: String, limit: Int): List<ClientStats>
suspend fun getNewClientsCount(startDate: String, endDate: String): Int
suspend fun getReturningClientsCount(startDate: String, endDate: String): Int

// Income Analytics
suspend fun getIncomeByDay(startDate: String, endDate: String): List<DayStats>
suspend fun getIncomeByDayOfWeek(startDate: String, endDate: String): Map<Int, Long> // Day of week -> Income
suspend fun getAverageCheck(startDate: String, endDate: String): Long
suspend fun getIncomeGrowth(startDate: String, endDate: String, previousStartDate: String, previousEndDate: String): PeriodComparison

// Visit Analytics
suspend fun getVisitsByDay(startDate: String, endDate: String): List<Pair<String, Int>> // dateKey -> count
suspend fun getVisitsByDayOfWeek(startDate: String, endDate: String): Map<Int, Int>
suspend fun getAverageVisitDuration(startDate: String, endDate: String): Int // minutes
suspend fun getVisitFrequencyDistribution(startDate: String, endDate: String): List<VisitFrequencyBucket>

// Insights
suspend fun getBestDayByIncome(startDate: String, endDate: String): DayStats?
suspend fun getBestMonthByIncome(year: Int): Pair<YearMonth, Long>?
suspend fun getMostFrequentClient(startDate: String, endDate: String): ClientStats?
suspend fun getPeakHours(startDate: String, endDate: String): List<Pair<Int, Int>> // hour -> visit count
```

### ViewModel State Extensions

```kotlin
data class IncomeDetailsState(
    val totalIncome: Long,
    val averageIncomePerDay: Long,
    val averageCheck: Long,
    val incomeGrowth: PeriodComparison?,
    val incomeByDay: List<DayStats>,
    val incomeByDayOfWeek: Map<Int, Long>,
    val topDays: List<DayStats>,
    val isLoading: Boolean
)

data class ClientsAnalyticsState(
    val totalClients: Int,
    val newClients: Int,
    val returningClients: Int,
    val averageIncomePerClient: Long,
    val clientStats: List<ClientStats>,
    val topClients: List<ClientStats>,
    val isLoading: Boolean
)

data class VisitsAnalyticsState(
    val totalVisits: Int,
    val averageVisitsPerDay: Float,
    val mostActiveDay: DayStats?,
    val averageVisitDuration: Int,
    val visitsByDay: List<Pair<String, Int>>,
    val visitsByDayOfWeek: Map<Int, Int>,
    val visitFrequencyDistribution: List<VisitFrequencyBucket>,
    val isLoading: Boolean
)

data class InsightsState(
    val bestDayByIncome: DayStats?,
    val bestMonthByIncome: Pair<YearMonth, Long>?,
    val clientWhoPaidMost: ClientStats?,
    val mostFrequentClient: ClientStats?,
    val mostLoadedDayOfWeek: Pair<Int, Long>?, // day of week -> income
    val leastLoadedDayOfWeek: Pair<Int, Long>?,
    val peakHours: List<Pair<Int, Int>>,
    val revenueTrends: List<String>, // Auto-generated insights
    val recommendations: List<String>,
    val isLoading: Boolean
)
```

---

## Chart Library Recommendation

**Recommended:** `vico` (by patrykandpatrick) or `compose-charts`

**Alternative:** Custom Compose Canvas implementation for full control

**Features needed:**
- Line charts
- Bar charts
- Pie/Donut charts
- Interactive (tap to highlight)
- Smooth animations
- Customizable colors

---

## Navigation Structure

```
StatsScreen (Dashboard)
├── Income Details Screen
│   └── Day Detail Screen (if clicking on day)
├── Clients Analytics Screen
│   └── Client Detail Screen (if clicking on client)
├── Visits Analytics Screen
│   └── Day Detail Screen (if clicking on day)
└── Reports & Insights Screen
    └── [Various detail screens based on click]
```

**Navigation routes:**
- `stats` - Main dashboard
- `stats/income/{period}/{dateKey}` - Income details
- `stats/clients/{period}/{dateKey}` - Clients analytics
- `stats/visits/{period}/{dateKey}` - Visits analytics
- `stats/insights/{period}/{dateKey}` - Reports & insights

---

## UX Guidelines

### Visual Hierarchy
1. **Large numbers** - Use `displayLarge` or `headlineLarge` typography
2. **Labels** - Use `bodyMedium` with `onSurfaceVariant` color
3. **Charts** - Full width, minimum height 200dp
4. **Cards** - Rounded corners, elevation, padding 16dp

### Color Scheme
- **Income:** Primary color (green/blue)
- **Expenses:** Error color (red)
- **Profit (positive):** Primary color
- **Profit (negative):** Error color
- **Neutral metrics:** Secondary color

### Interactions
- **Clickable cards** - Add ripple effect, elevation on press
- **Chart segments** - Highlight on tap, show tooltip
- **Lists** - Full row clickable, show chevron icon

### Loading States
- Show skeleton loaders for charts
- Show progress indicator for data cards
- Disable interactions during loading

### Empty States
- Show friendly message when no data
- Suggest actions (e.g., "Add your first appointment")
- Use illustrations/icons

---

## Implementation Phases

### Phase 1: Enhanced Dashboard
- Add clickable metric cards
- Add quick insights section
- Improve visual design
- Add trend indicators

### Phase 2: Income Details Screen
- Create new screen
- Implement line chart for income over time
- Add income by day of week bar chart
- Add top days list

### Phase 3: Clients Analytics Screen
- Create new screen
- Implement donut chart for client contribution
- Add top clients list
- Add client distribution chart

### Phase 4: Visits Analytics Screen
- Create new screen
- Implement visits over time chart
- Add visits by day of week
- Add visit frequency distribution

### Phase 5: Reports & Insights Screen
- Create new screen
- Implement auto-generated insights
- Add recommendations engine
- Add revenue trends analysis

### Phase 6: Polish & Optimization
- Add animations
- Optimize data loading
- Add caching
- Improve error handling

---

## Technical Considerations

### Performance
- Load data asynchronously
- Cache calculated statistics
- Use pagination for large lists
- Lazy load charts

### Data Accuracy
- Use Room transactions for complex queries
- Handle edge cases (empty data, single day, etc.)
- Round percentages appropriately
- Format currency consistently

### Accessibility
- Add content descriptions for charts
- Support screen readers
- Ensure sufficient color contrast
- Provide text alternatives for visual data

---

## Example Calculations

### Average Check
```kotlin
averageCheck = totalIncome / totalVisits
```

### Income Growth
```kotlin
growthPercentage = ((currentIncome - previousIncome) / previousIncome) * 100
```

### Client Income Percentage
```kotlin
clientIncomePercentage = (clientIncome / totalIncome) * 100
```

### Average Income per Day
```kotlin
averageIncomePerDay = totalIncome / workingDaysCount
```

---

## Future Enhancements

1. **Export to PDF/Excel** - Generate reports
2. **Email Reports** - Send weekly/monthly summaries
3. **Goal Setting** - Set income targets, track progress
4. **Predictions** - Forecast future income based on trends
5. **Comparison Tools** - Compare multiple periods side-by-side
6. **Custom Date Ranges** - Allow user to select custom periods
7. **Filters** - Filter by client, service type, payment status
8. **Charts Export** - Save charts as images

---

## Notes

- All monetary values stored in cents (Long), displayed in rubles
- Dates stored as "yyyy-MM-dd" strings (dateKey)
- Period calculations should handle timezone correctly
- Charts should be responsive to different screen sizes
- Consider dark theme support for all visualizations

