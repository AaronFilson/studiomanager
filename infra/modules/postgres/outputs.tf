output "fqdn" {
  value = azurerm_postgresql_flexible_server.this.fqdn
}

output "server_name" {
  value = azurerm_postgresql_flexible_server.this.name
}

output "database_name" {
  value = azurerm_postgresql_flexible_server_database.app.name
}

output "id" {
  value = azurerm_postgresql_flexible_server.this.id
}
