@Bean
public PagingQueryProvider queryProvider() {
    SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
    queryProvider.setDataSource(dataSource);
    queryProvider.setSelectClause("SELECT id, name, description");
    queryProvider.setFromClause("FROM my_table");
    queryProvider.setWhereClause("WHERE condition = 'value'");
    queryProvider.setSortKey("id");  // The column to sort by for pagination
    try {
        return queryProvider.getObject();
    } catch (Exception e) {
        throw new RuntimeException("Unable to create the query provider", e);
    }
}
