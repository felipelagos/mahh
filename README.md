# Multi-Armed Bandit-Based Hyper-Heuristics for Combinatorial Optimization Problems

This repository contains the source code and instances of the paper [Multi-Armed Bandit-Based Hyper-Heuristics for Combinatorial Optimization Problems] by F. Lagos and J. Pereira (https://doi.org/10.1016/j.ejor.2023.06.016).

Case study: The Vehicle Routing Problem with Time Windows (VRPTW).

## Instances

The instances can be found in folder `instances`. These instances are in two different formats, as a text file (.txt) and as a csv. The latter format is used to populate the tables when using a database. The database schema is defined in `table.sql`.
For text files, the Solomon 1987 instances are contained in the folder `solomon` and Gehring & Homberger 1999 instances in `gehring`.

## Source code

The source can be found in the folder `src`. It is written in Java and supported using Maven. The compilation and running of the code require Apache Maven 3.6.3 and Java version 17.0.7.

### How to clone

```bash
git clone https://github.com/felipelagos/mahh.git
cd mahh
```

### How to compile

```bash
mvn assembly:assembly
```

### How to run

From the mahh directory run, for example

```bash
java -jar target/runhyper.jar --file=instances/gehring/C110_1.TXT
```

To see all options run
```sh
java -jar target/runhyper.jar --help
```

### How to reproduce the computational results

The code contains all the algorithms and cases studied in the paper. The parameters used can be found in the paper, along with the computational configuration considered.

## How to cite

```bibtex
@article{lagos2023multi,
title = {Multi-armed bandit-based hyper-heuristics for combinatorial optimization problems},
journal = {European Journal of Operational Research},
year = {2023},
issn = {0377-2217},
doi = {https://doi.org/10.1016/j.ejor.2023.06.016},
url = {https://www.sciencedirect.com/science/article/pii/S0377221723004678},
author = {Felipe Lagos and Jordi Pereira}
}
```
