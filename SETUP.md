# JavaFX Setup Instructions

## The "Not found: javafx" Error

This error occurs because IntelliJ IDEA hasn't synchronized the sbt project yet. The `build.sbt` file is correctly configured with JavaFX dependencies.

## Solution

### Option 1: Reload sbt Project (Recommended)
1. In IntelliJ IDEA, open the **sbt** tool window (View → Tool Windows → sbt)
2. Click the **Reload sbt Project** button (circular arrow icon)
3. Wait for dependencies to download

### Option 2: Reimport Project
1. File → Close Project
2. File → Open → Select the project folder
3. Choose "Open as Project" and select "sbt project"
4. Wait for IntelliJ to import and sync

### Option 3: Manual Sync
1. Right-click on `build.sbt` in the project tree
2. Select "sbt" → "Reload sbt Project"

## Verification

After syncing, the JavaFX imports should resolve. You can verify by:
- Running `sbt compile` in terminal (should succeed)
- Checking that red underlines disappear in `GameBoardController.scala`

## Note

The code is correct. This is purely an IDE synchronization issue. The project will compile and run correctly once the IDE recognizes the sbt dependencies.

