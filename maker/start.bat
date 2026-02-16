@echo off
cd /d "%~dp0"
echo === NeoMUDMaker ===
echo Installing dependencies...
call npm install
echo Generating Prisma client...
call npx prisma generate
echo Starting dev server...
call npm run dev
pause
