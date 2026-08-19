module.exports = {
  timeout: 900000,
  retries: 0,
  outputDir: '/tmp/pwrun/test-results',
  use: {
    headless: true,
    viewport: { width: 1920, height: 1080 },
    screenshot: 'only-on-failure',
    ignoreHTTPSErrors: true,
    actionTimeout: 30000,
    navigationTimeout: 120000
  },
  workers: 1,
  reporter: [['list']]
};
