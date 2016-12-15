Reviews analysis
====

Dataset for analysis is taken from [Kaggle](https://www.kaggle.com/snap/amazon-fine-food-reviews) (Amazon Fine Food Reviews).
The following information was extracted from the dataset:
- 1000 most active users (profile names)
- 1000 most commented food items (item ids)
- 1000 most used words in the reviews

Also all the reviews were translated through fake Google Translate API.
It was assumed that Google Translate API has 200ms average response time and can handle at most 100 requests in parallel.
The goal was to translate all the reviews efficiently and cost effective (assuming that calls to Google Translate API are paid).