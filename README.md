# Archimedes
An enterprise-level self-service data retrieval tool.

## Why Archimedes?

Data visualization can be broken down into two steps: the first is to accurately locate a batch of desired data, and the second is to transform and present this data interactively in the desired chart or graphic style. It is difficult to do both of these steps well at once, and more products are skilled in the second step. We have considered a solution, which is to focus on solving the first step and leave the second step to Excel. This solution has been running for three years in several Fortune 500 companies, performing well with thousands of queries and downloads every day. The downloaded results are used as input for the second step, and ultimately form a report presented to various managers and decision-makers. Now, we have open-sourced our tool for handling the first step, hoping to help more people improve their data visualization skills.

When it comes to data extraction tools, why emphasize enterprise-level and user self-service? The IT department has completed many systems with suppliers, forming many databases. When the business department requests data from the IT department, the IT department often has to involve suppliers and hold many meetings before providing the data, which can take one or two weeks. The difficulty lies in the fact that there are many IT systems in the enterprise, and each database, table, row of data, and field has its own business and IT meaning. Even if it describes the same thing and object, there are multiple expressions in different periods and corners. Therefore, it is difficult to determine what is accurate when discussing it again, and each discussion ends up adding another layer of meaning, which is only one of the inputs for the next discussion.

To solve this problem, efforts need to be made on both the production and consumption sides. We first focus on the consumption side, specifically the small field of data extraction from existing systems.

After several years of operation, we have summarized the following issues that need to be considered and resolved as an enterprise-level user self-service data extraction tool. Each of these issues may seem small, but if several issues are not resolved, they will affect the user's work efficiency.

**In terms of data sources:**
- No need for professional database knowledge and SQL knowledge, intuitively present the database table in web form through a few clicks and fills, specifically displaying data in table form, supporting filtering and pagination.
- Support keeping information consistent with the data source, that is, tables and fields can be automatically synchronized even if they are added or deleted.
- No impact on the data source, only a read-only account is required.

**In terms of presenting data:**
- Support adding more intuitive and unified field aliases and field descriptions to the presented fields. After all, most database fields are composed of letter abbreviations, and the lack of unified naming at the enterprise level has caused great trouble for users.
- Support configuring which fields need to be displayed.
- Support configuring the display order of fields, and the display order of fields reflects the logical relationship in business meaning.
- Support configuring the fields used for ascending/descending sorting when displaying data, and data sorting reflects business logic.
- Support configuring the table column width of displayed fields. Limited screen space needs to intuitively display field values to avoid users having to widen the width to see important information.
- Support configuring images and files in displayed field values.

**In terms of filtering data:**
- Support configuring which fields are used as filter items, and which is the first filter item, which is the second, and so on.
- Support configuring multiple filtering methods (exact text matching, left fuzzy text matching, right fuzzy text matching, full fuzzy text matching, single-select drop-down list, multi-select drop-down list, cascading single-select drop-down list, date range, etc.), and allow configuring the candidate values of drop-down lists and cascading lists to facilitate user filtering.
- Support configuring default filter values, such as filtering out data for the current day by default, or filtering data for a certain region by default.

**In terms of exporting data:**
- Support exporting csv (supporting export of tens of millions of data) and Excel.
- Support asynchronous export, that is, even if the user closes the page or browser, the export task will continue to execute.
- Support exporting images and files in data.

**In terms of data pivot tables:**
- Support filling in input data in an Excel pivot table template made by the tool, and then use Excel's powerful ability to obtain a pivot table.

**In terms of data permissions:**
- In an enterprise, each user is probably assigned permissions to view certain types of data. The "what kind" here is determined by each enterprise itself, and the essence is that the enterprise has multiple resources, and these resources have some dimensions, and each dimension has some values. Each user is granted access to the content/resources that can be used in which resources and which dimensions and value ranges. For example, geography is a relatively common dimension, and each user is often allowed to access certain types of data in certain geographic areas.
- Abstract the concept of resource ownership, the concept of resource access control dimension hierarchy, and define interfaces. When applied specifically, use plugins to provide data permissions based on the actual implementation of each enterprise.

**In terms of data organization:**
- In an enterprise, different application/project groups often need to create multiple data on the tool, allowing other users to access/extract. Therefore, support the concept of applications, each application includes one or more data items, and it is best to have a hierarchical structure to facilitate grouping and arranging data according to intuitive business meaning. Also, support which users can see this application, and even which data items in this application.

**In terms of Open API:**
- Sometimes, an application wants to embed the ability to display data in its own system (it is not easy to develop a good one if quality is considered). Therefore, the tool should provide the ability to embed pages, allowing applications to quickly dock and users to see a standard data presentation effect when opening the page.
- Sometimes, an application wants to implement a general query for a certain data table. If developed by itself, it is not easy to consider the diversity of query conditions, control the returned allowed fields, and control data permissions, etc. It can consider directly using the tool's general data query ability.

**In other aspects:**
- Data from data sources often updates frequently, so sometimes the data itself is not suitable for consumption. Therefore, the tool can consider supporting the configuration of prompts that such data cannot be used during updates at a certain time.
- When each enterprise introduces the tool, it needs to consider where the tool's users come from and how to authenticate them. Therefore, the tool can consider implementing a plugin system to support docking with the enterprise's own user center/user source and SSO (Single Sign On) through plugins.
- The number of people who query and export each data item every day, the speed of queries, and other operational information also need to be recorded and viewed.


## Supported Databases

- MySQL
- Microsfot SQL Server
- ClickHouse