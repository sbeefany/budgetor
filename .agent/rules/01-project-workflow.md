---
trigger: always_on
---

# Project Workflow Rules

This document outlines the mandatory project development pipeline. All tasks must flow through this pipeline to ensure quality and consistency.
All tasks should complete accordint PRD! Always check decisions with PRD!

## The Pipeline

1. **Check Backlog**: Review the `backlog.md` file in the root directory. Look for tasks and verify their properties and status.
2. **Prioritize Backlog**: Ensure the tasks are ordered by priority. If necessary, engage the Product Manager or Team Lead skill to reprioritize.
3. **Get Next Task**: Select the highest priority task that is marked as `To Do` or `Ready for Design`.
3.1 *Create new branch*: create new branch  from develop. Branch should start called with task code (CORE-1.... for example)
4. **Architectural Design (Architect Skill)**: For new features or complex tasks, the Architect must define the system design, data schemas, and API contracts before coding begins.
5. **Develop (Developer Skill - Strict TDD)**:
    - **Abstractions First**: Create necessary interfaces/abstractions.
    - **Red**: Write tests that *must* fail (or fail to compile initially).
    - **Green**: Implement the actual logic to make the tests pass.
    - *Constraint*: You are strictly forbidden from altering the meaning of the tests just to make them pass without modifying the underlying business logic. The logic must prove the test correct.
6. **Testing (QA Engineer Skill)**: The AI (acting as QA) must pull the branch/code, ensure all automated tests pass, and provide a working build. 
    - The AI guarantees the automated checks are green.
    - The actual **manual testing** is performed by the human User. The AI provides instructions on how to run/test the current snapshot.
    - **Cleanup Rule**: The developer must delete any extra support log files generated during debugging before marking a task as ready for review.
7. **Review**
    - After completing development and automated testing, the AI must change the task status to `review` in `backlog.md`.
    - Provide the user with the result to review. Wait for the user's decision.
8. **Feedback**: 
    - If the user approves (manual testing passes / review is OK), the AI must automatically change the task status to `complete` in `backlog.md`, and only then **commit and push the task**.
    - If manual testing fails or the user requests changes, a **Bug** is created in the backlog, prioritized, and sent back through the pipeline, or changes are made directly in the active branch.