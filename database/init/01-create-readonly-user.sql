-- Create read-only user for SQL query execution
-- This user is used exclusively by SqlSafetyService / QueryExecutionService
CREATE USER IF NOT EXISTS 'app_readonly'@'%' IDENTIFIED BY 'readonly_pass_2026';

-- Grant SELECT only on all tables in agent_analysis
GRANT SELECT ON agent_analysis.* TO 'app_readonly'@'%';

-- Grant EXECUTE (needed for EXPLAIN)
GRANT EXECUTE ON agent_analysis.* TO 'app_readonly'@'%';

FLUSH PRIVILEGES;
