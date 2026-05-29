module.exports = {
  timeout: 180000,
  retries: 0,
  // Keep all transient output (traces, failure shots, .last-run.json)
  // out of the bind-mounted Jenkins workspace.
  outputDir: '/tmp/pwrun/test-results',
  use: {
    headless: true,
    viewport: { width: 1920, height: 1080 },
    screenshot: 'only-on-failure',
    ignoreHTTPSErrors: true
  }
};
