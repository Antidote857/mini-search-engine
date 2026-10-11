# Spy-der

A Java console search engine that searches local text documents using an inverted index, term-frequency scoring, and ranked results.

## Features

- Load UTF-8 `.txt` documents from a local folder.
- Normalize text and queries using the same processing rules.
- Remove common stop words.
- Build an inverted index containing document term frequencies.
- Search using single or multiple words.
- Rank matching documents by relevance score.
- Display matched terms, indexed vocabulary, and document filenames.
- Handle invalid queries, missing folders, and unreadable files.
- Verify core behavior through 20 automated test scenarios.

## Requirements

- JDK 21
- A terminal such as Windows PowerShell

No external runtime dependencies are required.

## Getting started

Clone the repository:

```powershell
git clone https://github.com/Antidote857/mini-search-engine.git
cd mini-search-engine
```

Compile:

```powershell
javac -d out -sourcepath src src\Main.java
```

If compilation succeeds, run:

```powershell
java -cp out Main
```

Run these commands from the project root so the application can locate
the `documents` folder.

## Using Spy-der

The console menu provides:

1. Search documents
2. View indexed terms
3. View document list
0. Exit

Press Enter after an action to return to the menu.

Add UTF-8 `.txt` files directly inside `documents`, then restart the
application to rebuild the index. Subfolders are not searched.

The application reads documents without editing or deleting them.

## Example search

With the included sample documents:

```text
Search results for: java collections
Matching documents: 3

1. java.txt
   Score: 6
   Matched terms: java, collections

2. software-development.txt
   Score: 2
   Matched terms: java, collections

3. databases.txt
   Score: 1
   Matched terms: java
```

## How searching works

### Text processing

Document content and search queries follow the same rules:

1. Convert text to lowercase using `Locale.ROOT`.
2. Replace characters other than letters and numbers with spaces.
3. Split on whitespace.
4. Remove configured stop words.

Repeated document words are retained for frequency counting.
Repeated query terms are counted once, so `java java` produces the
same results as `java`.

### Inverted index

The index uses this structure:

```java
Map<String, Map<Integer, Integer>>
```

It maps:

```text
term -> document ID -> occurrence count
```

This lets the search engine look up documents containing a query term
without scanning every document's full content for each search.

### Relevance scoring

A document's score is the sum of the frequencies of its matching
unique query terms.

For example:

```text
java:        4 occurrences
collections: 2 occurrences
score:       6
```

Multi-word searches match one or more query terms. Matching every
term is not required.

Results are sorted by:

1. Highest score
2. Filename in natural, case-sensitive order
3. Document ID

Raw frequency scoring can favour longer documents or documents that
repeat one term frequently. Matching more distinct terms does not
automatically guarantee a higher rank.

## Project structure

- `src/Main.java` — startup, console menu, and result display
- `src/model/Document.java` — immutable document data
- `src/model/SearchResult.java` — document, score, and matched terms
- `src/service/DocumentLoader.java` — text file discovery and loading
- `src/service/TextProcessor.java` — normalization and tokenization
- `src/service/SearchIndexer.java` — inverted index and document lookup
- `src/service/SearchEngine.java` — query processing, scoring, and ranking
- `src/util/StopWords.java` — stop-word membership checks
- `documents/` — sample text documents
- `tests/SearchEngineTest.java` — automated test runner
- `assets/` — demonstration images

## Automated tests

Compile the tests:

```powershell
javac -d out-test -sourcepath src tests\SearchEngineTest.java
```

If compilation succeeds, run:

```powershell
java -cp out-test SearchEngineTest
```

Expected summary:

```text
Results: 20 passed, 0 failed.
```

Tests cover:

- Document validation and file loading
- UTF-8 content and file filtering
- Empty directories and invalid paths
- Invalid UTF-8 files
- Case conversion, punctuation, and stop words
- Term frequencies and index replacement
- Duplicate document IDs
- Single-term and multi-term searches
- Query normalization and repeated query terms
- Unknown and empty searches
- Scoring and equal-score ordering
- Unmodifiable returned collections
- Loading, indexing, and searching together

Tests create and remove their own temporary files. A warning about
`bad.txt` is expected during the invalid UTF-8 test.

The test runner exits with a nonzero code if any scenario fails.

## Design decisions

- `HashMap` stores document lookups, index postings, and scores.
- `LinkedHashSet` removes duplicate query terms while retaining query order.
- `ArrayList` collects tokens, documents, and results.
- `Comparator` provides predictable ranking and display ordering.
- Index rebuilding uses new maps, replacing the existing index only
  after all supplied documents are successfully processed.
- Document and result data are immutable, and index accessors return
  protected collections.

## Current limitations

- Searches local `.txt` files only.
- Stores documents and the index in memory.
- Requires a restart to pick up document changes.
- Uses exact normalized terms without stemming or fuzzy matching.
- Does not support phrase searches or Boolean query operators.
- Uses raw term frequency without document-length normalization.
- Provides a single-threaded console interface.
- Does not search the internet.

## Future improvements

- TF-IDF scoring
- Phrase searching and search suggestions
- Matching-text previews and highlighting
- Automated continuous integration
- Performance benchmarking
- JavaFX desktop interface
- Java REST API with an HTML, CSS, and JavaScript frontend

## Demonstration

![Spy-der showing ranked results for java collections](assets_search_demo.png)