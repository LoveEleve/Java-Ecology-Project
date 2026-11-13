-- 演示数据：活动 A1，市场部/产品部两个来源，T1 券模板子预算。
INSERT INTO t_budget_account
(activity_id, source_id, template_id, unit, total_amount, reserved_amount, consumed_amount, budget_version, updated_at)
VALUES
('A1', 'market', '__TOTAL__', 'AMOUNT', 1000.0000, 0, 0, 0, CURRENT_TIMESTAMP),
('A1', 'market', 'T1',        'AMOUNT',  300.0000, 0, 0, 0, CURRENT_TIMESTAMP),
('A1', 'product', '__TOTAL__', 'AMOUNT', 5000.0000, 0, 0, 0, CURRENT_TIMESTAMP),
('A1', 'product', 'T1',        'AMOUNT', 2000.0000, 0, 0, 0, CURRENT_TIMESTAMP);
