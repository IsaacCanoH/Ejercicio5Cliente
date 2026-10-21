const { defineConfig } = require('playwright/test');
const path = require('node:path');

const run = process.env.P2_RUN || 'final';
const output = path.resolve(__dirname, '../output/p2/automaticas', run);

module.exports = defineConfig({
  testDir: './tests/automaticas',
  timeout: 20000,
  expect: { timeout: 4000 },
  workers: 1,
  retries: 0,
  fullyParallel: false,
  outputDir: path.join(output, 'artefactos'),
  reporter: [
    ['list'],
    ['html', { outputFolder: path.join(output, 'html'), open: 'never' }],
    ['json', { outputFile: path.join(output, 'resultados.json') }],
    ['junit', { outputFile: path.join(output, 'resultados.xml') }]
  ],
  use: {
    baseURL: process.env.FRONTEND_URL || 'http://localhost:8080',
    browserName: 'chromium',
    channel: process.env.P2_BROWSER || 'chrome',
    headless: true,
    viewport: { width: 1440, height: 1000 },
    screenshot: 'on',
    trace: 'retain-on-failure'
  }
});
