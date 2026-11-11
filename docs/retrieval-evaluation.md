# Retrieval evaluation

`retrieval-evaluation.jsonl` is a small synthetic starter set. Each record defines a search query and relevant source/page labels. The PDF names and labels are fictional; create synthetic fixture documents matching those labels when final verification is scheduled. Do not evaluate against uploaded personal documents.

For each query, record the top 5 citations and calculate:

- **Recall@5**: whether any labelled relevant page appears in the first five results.
- **MRR**: the reciprocal rank of the first relevant page, or zero if absent.
- **Citation validity**: whether each returned page/offset range points into the indexed text for the cited source.
- **Workspace isolation**: whether a second workspace can retrieve none of the first workspace's chunks.

Compare lexical-only results with hybrid results after explicitly enabling embeddings. Record the configured model and date with the scores. Model calls are intentionally not part of ordinary compilation or local startup; budget/API usage must be considered before enabling them.
