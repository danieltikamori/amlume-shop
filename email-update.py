# #!/usr/bin/env python3

# Script to replace your email address with the one you want to use in old commits. Timeshift will be used to update the email address in the commit history.
# INSTRUCTIONS:
# Replace my_email@mail_domain.com with your real email address
# Replace base64_encoded_email_address with the base64 encoded email address

# def replace_email(blob):
#     data = blob.data
#     if b'Copyright' in data:
#         old_data = data
#         data = data.replace(
#             b'my_email@mail_domain.com',
#             b'echo base64_encoded_email_address | base64 -d && echo'
#         )
#         if data != old_data:
#             blob.data = data
#             return True
#     return False

# import git_filter_repo as gfr
# settings = gfr.FilteringOptions.default_options()
# settings.force = True
# filter = gfr.RepoFilter(settings)
# # The correct way to register the callback
# gfr.Blob.callback = replace_email
# filter.run()

#!/usr/bin/env python3
import git_filter_repo as gfr
import time
import os
import subprocess

def replace_email(blob, metadata):
    data = blob.data
    if b'Copyright' in data:
        new_data = data.replace(
            b'my_email@mail_domain.com',
            b'echo base64_encoded_email_address | base64 -d && echo'
        )
        if new_data != data:
            blob.data = new_data
            return True
    return False

def run_git_command(command):
    try:
        subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        return True
    except subprocess.CalledProcessError as e:
        print(f"Error running command '{command}': {e.stderr}")
        return False

def main():
    settings = gfr.FilteringOptions.default_options()
    settings.force = True

    cache_dir = os.path.expanduser("~/.cache/git-filter-repo")
    if os.path.exists(cache_dir):
        print(f"Clearing git-filter-repo cache: {cache_dir}")
        import shutil
        try:
            shutil.rmtree(cache_dir)
        except OSError as e:
            print(f"Error clearing cache: {e}")

    if not run_git_command("git reflog expire --expire=now --all"):
        print("Failed to run git reflog.")
    if not run_git_command("git gc --prune=now --aggressive"):
        print("Failed to run git gc.")

    max_retries = 3
    for attempt in range(max_retries):
        try:
            f = gfr.RepoFilter(settings, blob_callback=replace_email)  # Correct way to use blob_callback
            f.run()
            break  # Exit loop if successful
        except AssertionError as e:
            print(f"git-filter-repo failed with AssertionError: {e}")
            if attempt < max_retries - 1:
                print(f"Retry attempt {attempt + 1} of {max_retries}")
                time.sleep(2)
            else:
                print("Failed after maximum retry attempts")
                raise  # Re-raise the exception after max retries
        except PermissionError as e:
            if attempt < max_retries - 1:
                print(f"Retry attempt {attempt + 1} of {max_retries}")
                time.sleep(2)
            else:
                print("Failed after maximum retry attempts")
                raise  # Re-raise it

if __name__ == '__main__':
    main()