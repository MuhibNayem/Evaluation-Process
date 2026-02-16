-- ============================================================================
-- V2: System settings and campaign-level overrides
-- ============================================================================

-- System-wide settings (admin-configurable)
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key     VARCHAR(100)    PRIMARY KEY,
    setting_value   TEXT            NOT NULL,
    category        VARCHAR(30)     NOT NULL,
    description     TEXT,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_settings_category ON system_settings(category);

-- Campaign-level setting overrides
CREATE TABLE IF NOT EXISTS campaign_setting_overrides (
    campaign_id     VARCHAR(36)     NOT NULL REFERENCES campaigns(id),
    setting_key     VARCHAR(100)    NOT NULL REFERENCES system_settings(setting_key),
    setting_value   TEXT            NOT NULL,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    PRIMARY KEY (campaign_id, setting_key)
);

CREATE INDEX idx_campaign_overrides_campaign ON campaign_setting_overrides(campaign_id);

-- Seed default system settings
INSERT INTO system_settings (setting_key, setting_value, category, description, updated_by, updated_at) VALUES
    ('scoring.default-method',              'WEIGHTED_AVERAGE', 'SCORING',      'Default scoring method when none is specified',            'system', NOW()),
    ('scoring.passing-score-threshold',     '70.0',             'SCORING',      'Score threshold (%) to consider an evaluation passing',    'system', NOW()),
    ('scoring.auto-score-on-submit',        'true',             'SCORING',      'Automatically score evaluations on submission',            'system', NOW()),
    ('scoring.enable-partial-credit',       'true',             'SCORING',      'Enable partial credit for partially correct answers',      'system', NOW()),
    ('scoring.max-score-value',             '100.0',            'SCORING',      'Maximum score value for normalization',                    'system', NOW()),
    ('campaign.auto-activate',              'false',            'CAMPAIGN',     'Auto-activate campaigns at their start date',              'system', NOW()),
    ('campaign.auto-close',                 'false',            'CAMPAIGN',     'Auto-close campaigns at their end date',                   'system', NOW()),
    ('campaign.max-deadline-extension-days','30',               'CAMPAIGN',     'Maximum days a campaign deadline can be extended',          'system', NOW()),
    ('campaign.default-minimum-respondents','1',                'CAMPAIGN',     'Default minimum respondents for a campaign',               'system', NOW()),
    ('campaign.send-deadline-reminders',    'true',             'CAMPAIGN',     'Send reminder notifications before deadline',              'system', NOW()),
    ('campaign.reminder-days-before-deadline','3',              'CAMPAIGN',     'Days before deadline to send reminders',                   'system', NOW()),
    ('notification.enabled',                'true',             'NOTIFICATION', 'Enable or disable notifications globally',                 'system', NOW()),
    ('features.allow-anonymous-mode',       'true',             'FEATURES',    'Allow anonymous submission mode',                          'system', NOW()),
    ('features.enable-reports',             'true',             'FEATURES',    'Enable report generation',                                 'system', NOW()),
    ('features.enable-csv-export',          'true',             'FEATURES',    'Enable CSV export',                                        'system', NOW()),
    ('features.enable-pdf-export',          'false',            'FEATURES',    'Enable PDF export',                                        'system', NOW()),
    ('pagination.default-page-size',        '20',               'PAGINATION',  'Default number of items per page',                         'system', NOW()),
    ('pagination.max-page-size',            '100',              'PAGINATION',  'Maximum allowed page size',                                'system', NOW())
ON CONFLICT (setting_key) DO NOTHING;
