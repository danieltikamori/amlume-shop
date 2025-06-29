# Git Commit Best Practices

## Issue Summary

Recently, a commit with the message "Add EncryptionException for handling encryption/decryption failures in auth server"
accidentally included several undesired files. The commit was intended to only include files related to the
EncryptionException functionality, but it ended up including 578 files with 11,636 insertions and 259 deletions.

## Best Practices to Avoid Similar Issues

### 1. Review Changes Before Committing

Always review what files are staged for commit before executing the commit command:

```bash
git status
```

For a more detailed view of the changes:

```bash
git diff --staged
```

### 2. Stage Files Selectively

Instead of using `git add .` or `git add -A`, stage files selectively:

```bash
git add path/to/specific/file.java
```

### 3. Use Interactive Staging

For more control over what gets staged, use interactive staging:

```bash
git add -i
```

This allows you to select specific files or even specific parts of files to stage.

### 4. Create Atomic Commits

Each commit should represent a single logical change. If you're working on multiple features or fixes, commit them
separately.

### 5. Use Git Hooks

Consider setting up pre-commit hooks to prevent committing unintended files or to enforce commit message standards.

### 6. Use Git GUI Tools

Git GUI tools like GitKraken, SourceTree, or the built-in Git integration in IDEs like IntelliJ IDEA can make it easier
to review and selectively stage changes.

### 7. Commit Message Guidelines

- Use a concise summary line (50 characters or less)
- Provide a detailed description if necessary
- Reference issue numbers if applicable
- Use the imperative mood ("Add feature" not "Added feature")

Example:

```
Add EncryptionException for auth server

- Create EncryptionException class for handling encryption/decryption failures
- Add exception handler in GlobalSecurityExceptionHandler
- Implement in AesGcmEncryptionService and related classes

Fixes #123
```

### 8. Verify Commit History

After committing, verify that your commit history looks as expected:

```bash
git log --stat
```

This shows a summary of files changed in each commit.

## Recovering from Mistakes

If you've already made a commit with unintended changes:

1. If the commit hasn't been pushed:
   ```bash
   git reset --soft HEAD~1  # Undo the commit but keep changes staged
   ```

2. If the commit has been pushed:
   ```bash
   git revert <commit-hash>  # Create a new commit that undoes the changes
   ```

See the accompanying `instructions-for-fixing-commit.md` file for detailed steps on how to fix the specific issue with
the EncryptionException commit.

## Conclusion

Taking a few extra moments to review changes before committing can save significant time and effort that would otherwise
be spent fixing commit mistakes. By following these best practices, we can maintain a clean and meaningful commit
history that accurately reflects the evolution of our codebase.
