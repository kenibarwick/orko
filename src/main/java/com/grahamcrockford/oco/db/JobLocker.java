package com.grahamcrockford.oco.db;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.grahamcrockford.oco.OcoConfiguration;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

@Singleton
public class JobLocker {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobLocker.class);

  private final Supplier<DBCollection> lock = Suppliers.memoize(this::createLockCollection);
  private final MongoClient mongoClient;
  private final OcoConfiguration configuration;

  @Inject
  JobLocker(MongoClient mongoClient, OcoConfiguration configuration) {
    this.mongoClient = mongoClient;
    this.configuration = configuration;
  }

  public boolean attemptLock(String jobId, UUID uuid) {

    BasicDBObject doc = new BasicDBObject()
        .append("_id", new ObjectId(jobId))
        .append("ts", Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
        .append("aid", uuid.toString());
    try {
      lock.get().insert(doc);
      return true;
    } catch (DuplicateKeyException e) {
      return false;
    }
  }

  public void releaseLock(String jobId, UUID uuid) {
    BasicDBObject query = new BasicDBObject()
        .append("_id", new ObjectId(jobId))
        .append("aid", uuid.toString());
    lock.get().remove(query);
  }

  public void releaseAnyLock(String jobId) {
    BasicDBObject query = new BasicDBObject()
        .append("_id", new ObjectId(jobId));
    lock.get().remove(query);
  }

  public void releaseAllLocks() {
    lock.get().remove(new BasicDBObject());
  }

  public boolean updateLock(String jobId, UUID uuid) {
    BasicDBObject query = new BasicDBObject()
        .append("_id", new ObjectId(jobId))
        .append("aid", uuid.toString());
    BasicDBObject update = new BasicDBObject()
        .append("$set", new BasicDBObject().append("ts", Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))));
    try {
      WriteResult result = lock.get().update(query, update);
      if (result.getN() != 1) {
        LOGGER.info("Job id " + jobId + " lost lock.");
        return false;
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Job id " + jobId + " lost lock.", e);
      return false;
    }
  }

  private DBCollection createLockCollection() {
    DBCollection exclusiveLock = mongoClient.getDB(DbModule.DB_NAME).getCollection("job").getCollection("lock");
    createTtlIndex(exclusiveLock);
    createAidIndex(exclusiveLock);
    return exclusiveLock;
  }

  private void createAidIndex(DBCollection exclusiveLock) {
    BasicDBObject index = new BasicDBObject();
    index.put("aid", 1);
    BasicDBObject indexOpts = new BasicDBObject();
    indexOpts.put("unique", false);
    exclusiveLock.createIndex(index, indexOpts);
  }

  private void createTtlIndex(DBCollection exclusiveLock) {
    BasicDBObject index = new BasicDBObject();
    index.put("ts", 1);
    BasicDBObject indexOpts = new BasicDBObject();
    indexOpts.put("expireAfterSeconds", configuration.getLockSeconds());
    exclusiveLock.createIndex(index, indexOpts);
  }
}