# ie-benchmarks
speed benchmarks for information extraction systems


## Data

We provide a random sample of 5K and 10K articles from the [English Wikipedia](https://en.wikipedia.org/wiki/English_Wikipedia) for use in benchmarking.  

These articles were processed using the `ie-benchmarks` branch of [`wikiparse`](https://github.com/clulab/wikiparse/tree/ie-benchmarks), which in turn uses the `org.clulab.processor.CluProcessor` ([v7.51](https://github.com/clulab/processors/releases/tag/v7.5.1)) text annotator for sentence segmentation, tokenization, part of speech tagging, lemmatization, chunking, named entity recognition, and dependency parsing.

As of this writing, the following link will retrieve the most recently completed dump of the English Wikipedia:

- https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2

A select number of recent dumps can be found at https://dumps.wikimedia.org/enwiki/.

The sample datasets released here were generated from the __June 2, 2014__ dump of the English Wikipedia. 

#### 5K article sample
```bash
# Download a random sample of 5K EN Wikipedia articles
curl https://public.lum.ai/ie-benchmarks/parsed-documents/wikipedia/en/5K.tar.gz --output 5K.tar.gz
# unpack the archive
tar xvzf 5K.tar.gz
```

#### 10K article sample

```bash
# Download a random sample of 10K EN Wikipedia articles
curl https://public.lum.ai/ie-benchmarks/parsed-documents/wikipedia/en/10K.tar.gz --output 10K.tar.gz
# unpack the archive
tar xvzf 10K.tar.gz
```

## Odinson
### Building an Odinson index


#### 5K article sample
```bash
sbt "odinson/runMain ai.lum.benchmarks.odinson.IndexDocuments -i 5K -o 5k-index"
```
#### 10K article sample

```bash
sbt "odinson/runMain ai.lum.benchmarks.odinson.IndexDocuments -i 10K -o 10k-index"
```

### Benchmarking

```bash
sbt "odinson/runMain ai.lum.benchmarks.odinson.BenchmarkQueries -i 5k-index -q example-1.odinson -n 1000 -o output/5k/odinson"
```


## Odin

### Benchmarking

```bash
sbt "odin/runMain ai.lum.benchmarks.odin.BenchmarkQueries -d 5K -g queries/odin/system.yml -n 1000 -o output/5k/odin"
```