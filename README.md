# graphHadoop


# Single-Source Shortest Path on Twitter Follower Graph with Map-Reduce
This project implements the single-source shortest path algorithm using Map-Reduce to find the minimum follower distance from a starting user on a Twitter follower graph.

## Dataset:
- Real Twitter data from 2010 ([source to be added later])
- Follower graph represented as user_id, follower_id pairs (one line per edge)
- Available on Expanse: /expanse/lustre/projects/uot189/fegaras/large-twitter.csv
- Contains ~736,930 users and ~36.7 million edges

## Algorithm:
- Iterative approach to find shortest distances
- Leverages Map-Reduce for parallel processing
- Repeatedly processes the graph to update distances

## Input Format:
Directed graph with one edge per line
Example:
2,1
3,1
3,2
...

## Output Format:
List of vertices and their distances from the starting vertex (unreachable vertices excluded)
Example (for starting vertex 1):
10 21 31 42 52 63

## Steps:
- Preprocessing: Convert the graph into a format suitable for Map-Reduce (each vertex with its following IDs)
- Shortest Path Calculation (Iterative):
  - Map-Reduce Job 1: Propagate distances from followers to a vertex
  - Repeat this job for multiple iterations (typically 5)
- Result Selection: Select vertices reachable from the starting point within a specific number of steps (e.g., 5)

## Implementation Details:
- Tagged class: Represents a vertex with distance and follower information (used for passing data in Map-Reduce)
- Pseudocode provided for key Map and Reduce functions

## Running the Project:
Specific instructions and dependencies will be provided in separate documentation.

## Note:
This document serves as a high-level overview.

