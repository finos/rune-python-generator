#!/bin/bash

repo=$1

# Ensure the repo argument is provided
if [[ -z "$repo" ]]; then
  echo "Error: Repository not specified."
  echo "Usage: gh_bulk_delete_workflow_runs <owner/repo>"
  exit 1
fi

# Fetch all workflow runs for the given repository 
runs=$(gh api repos/$repo/actions/runs --paginate | jq -r '.workflow_runs[] | .id')

if [[ -z "$runs" ]]; then
  echo "No workflow runs found for $repo."
  exit 0
fi

while IFS= read -r run; do
  echo "Deleting run $run..."
  gh api -X DELETE repos/$repo/actions/runs/$run --silent
done <<< "$runs"

echo "All workflow runs for $repo have been deleted."