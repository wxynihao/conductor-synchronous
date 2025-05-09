a = "123^456".split('\\^')
        .collect { id -> [redis_query: [cacheName: 'data.hdfs.cache', command: "hget 日游_${id} score"]] }

print(a)