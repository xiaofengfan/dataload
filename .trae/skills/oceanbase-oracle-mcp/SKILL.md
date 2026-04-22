---
name: "oceanbase-oracle-mcp"
description: "Connect to OceanBase Oracle mode database via MCP. Invoke when user wants to query or manage OceanBase Oracle database, or needs database connection for AI interactions."
---

# OceanBase Oracle MCP

This skill provides a Model Context Protocol (MCP) server connection to OceanBase Oracle mode database.

## Connection Details

From the application's `application.yml`:
- **Host**: 120.55.98.148
- **Port**: 2881
- **Username**: sys@oracle_db
- **Database**: sys (Oracle mode)

## Setup Options

### Option 1: Using @ggball/mcp-database (Recommended)

Install the npm package globally:
```bash
npm install -g @ggball/mcp-database
```

Configure in Claude Desktop (`%APPDATA%\Claude\claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "oceanbase-oracle": {
      "command": "mcp-database",
      "env": {
        "DB_TYPE": "oceanbase",
        "DB_HOST": "120.55.98.148",
        "DB_PORT": "2881",
        "DB_USER": "sys@oracle_db",
        "DB_PASSWORD": "change_on_install",
        "DB_DATABASE": "sys",
        "DB_CHARSET": "UTF8"
      }
    }
  }
}
```

### Option 2: Using Python mcp-oceanbase

Clone and setup:
```bash
git clone https://github.com/oceanbase/mcp-oceanbase.git
cd mcp-oceanbase
uv venv
source .venv/bin/activate
uv pip install -e .
```

Configure `.env` file:
```
OCEANBASE_HOST=120.55.98.148
OCEANBASE_PORT=2881
OCEANBASE_USER=sys@oracle_db
OCEANBASE_PASSWORD=change_on_install
OCEANBASE_DATABASE=sys
```

Run the server:
```bash
python -m src.oceanbase_mcp_server.server
```

## Available Tools

Once connected, available tools include:
- `execute_sql` - Execute SELECT queries
- `list_tables` - List all tables in the database
- `describe_table` - Get table structure information
- `search_schema` - Search for tables/columns by keyword

## Verification

Test connection with:
```bash
mcp-database --test
```

Or query directly:
```sql
SELECT 1 FROM DUAL
```
