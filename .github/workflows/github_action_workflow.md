name: Generate Missing Drawable Icons

# This workflow is triggered manually from the Actions tab
on:
  workflow_dispatch:

jobs:
  generate-and-commit:
    runs-on: ubuntu-latest
    permissions:
      contents: write # Required to commit back to the repository

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.10'

      - name: Run Python Script
        run: python generate_icons.py

      - name: Commit and Push Changes
        run: |
          git config --global user.name "github-actions[bot]"
          git config --global user.email "github-actions[bot]@users.noreply.github.com"
          git add composeApp/src/commonMain/composeResources/drawable/*.xml
          
          # Only commit and push if there are actual changes
          if git diff --staged --quiet; then
            echo "No new icons to commit."
          else
            git commit -m "chore: auto-generate missing placeholder drawable xmls"
            git push
          fi