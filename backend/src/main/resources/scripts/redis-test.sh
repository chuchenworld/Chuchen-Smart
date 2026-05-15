#!/bin/bash

# Redis 测试脚本
# 用于测试 Redis 连接和存储功能

echo "=== Redis 连接测试 ==="
redis-cli -h 115.29.169.132 -p 6379 -a root ping

echo ""
echo "=== 查看所有 keys ==="
redis-cli -h 115.29.169.132 -p 6379 -a root keys "*"

echo ""
echo "=== 测试存储简单值 ==="
redis-cli -h 115.29.169.132 -p 6379 -a root set test_key "test_value"
redis-cli -h 115.29.169.132 -p 6379 -a root get test_key

echo ""
echo "=== 查看所有 keys（包含新存储的）==="
redis-cli -h 115.29.169.132 -p 6379 -a root keys "*"
