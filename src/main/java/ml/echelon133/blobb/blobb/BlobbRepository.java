package ml.echelon133.blobb.blobb;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlobbRepository extends Neo4jRepository<Blobb, UUID> {

    /*
        User's feed consists of most recent blobbs/reblobbs/responses that have been posted
        either by themselves or users that they follow.

        This query can be simplified if every user follows themselves by default. Then it can be performed as a single case.
        Other queries that count/list follows/followers should hide the fact that the user follows themselves by filtering
        results.
     */
    @Query( "MATCH (u:User)-[:FOLLOWS]->(poster:User)-[:POSTS]->(blobbs:Blobb) " +
            "WHERE u.uuid = $uuid AND blobbs.creationDate >= $first AND blobbs.creationDate <= $second AND blobbs.deleted <> true " +
            "OPTIONAL MATCH (blobbs:Blobb)-[:RESPONDS]->(respondsTo:Blobb) " +
            "OPTIONAL MATCH (blobbs:Blobb)-[:REBLOBBS]->(reblobbs:Blobb) " +
            "RETURN blobbs.uuid AS uuid, blobbs.content AS content, blobbs.creationDate AS date, poster AS author, " +
            "reblobbs.uuid AS reblobbs, respondsTo.uuid AS respondsTo " +
            "ORDER BY datetime(blobbs.creationDate) DESC SKIP $skip LIMIT $limit ")
    List<FeedBlobb> getFeedForUserWithUuid_PostedBetween(UUID uuid, Date first, Date second, Long skip, Long limit);

    @Query( "MATCH (u:User)-[:FOLLOWS]->(poster:User)-[:POSTS]->(blobbs:Blobb) " +
            "WHERE u.uuid = $uuid AND blobbs.creationDate >= $first AND blobbs.creationDate <= $second AND blobbs.deleted <> true " +
            "OPTIONAL MATCH (blobbs:Blobb)-[:RESPONDS]->(respondsTo:Blobb) " +
            "OPTIONAL MATCH (blobbs:Blobb)-[:REBLOBBS]->(reblobbs:Blobb) " +
            "OPTIONAL MATCH (:User)-[l:LIKES]->(blobbs) " +
            "WITH blobbs, reblobbs, respondsTo, poster, count(l) as amountLikes " +
            "RETURN blobbs.uuid AS uuid, blobbs.content AS content, blobbs.creationDate AS date, poster AS author, " +
            "reblobbs.uuid AS reblobbs, respondsTo.uuid AS respondsTo " +
            "ORDER BY amountLikes DESC, datetime(blobbs.creationDate) DESC SKIP $skip LIMIT $limit ")
    List<FeedBlobb> getFeedForUserWithUuid_Popular_PostedBetween(UUID uuid, Date first, Date second, Long skip, Long limit);

    @Query( "MATCH (u:User)-[:POSTS]->(blobb:Blobb) WHERE blobb.uuid = $uuid AND blobb.deleted <> true " +
            "OPTIONAL MATCH (blobb:Blobb)-[:RESPONDS]->(respondsTo:Blobb) " +
            "OPTIONAL MATCH (blobb:Blobb)-[:REBLOBBS]->(reblobbs:Blobb) " +
            "RETURN blobb.uuid AS uuid, blobb.content AS content, blobb.creationDate AS date, u AS author, " +
            "reblobbs.uuid AS reblobbs, respondsTo.uuid AS respondsTo ")
    Optional<FeedBlobb> getBlobbWithUuid(UUID uuid);

    @Query( "MATCH (blobb:Blobb) WHERE blobb.uuid = $uuid AND blobb.deleted <> true " +
            "OPTIONAL MATCH (:User)-[likes:LIKES]->(blobb:Blobb) " +
            "OPTIONAL MATCH (res:ResponseBlobb)-[responses:RESPONDS]->(blobb) WHERE res.deleted <> true " +
            "OPTIONAL MATCH (reb:Reblobb)-[reblobbs:REBLOBBS]->(blobb:Blobb) WHERE reb.deleted <> true " +
            "RETURN blobb.uuid AS uuid, count(distinct(responses)) AS responses, count(distinct(likes)) AS likes, " +
            "count(distinct(reblobbs)) AS reblobbs")
    Optional<BlobbInfo> getInfoAboutBlobbWithUuid(UUID uuid);

    @Query( "MATCH (u:User) WHERE u.uuid = $uuidOfUser " +
            "MATCH (b:Blobb) WHERE b.uuid = $uuidOfBlobb AND b.deleted <> true " +
            "CREATE (u)-[l:LIKES]->(b) " +
            "RETURN id(l)")
    Optional<Long> likeBlobbWithUuid(UUID uuidOfUser, UUID uuidOfBlobb);

    @Query( "MATCH (u:User)-[l:LIKES]->(b:Blobb)" +
            "WHERE u.uuid = $uuidOfUser AND b.uuid = $uuidOfBlobb AND b.deleted <> true " +
            "RETURN id(l)")
    Optional<Long> checkIfUserWithUuidLikes(UUID uuidOfUser, UUID uuidOfBlobb);

    @Query( "MATCH (u:User)-[l:LIKES]->(b:Blobb) " +
            "WHERE u.uuid = $uuidOfUser AND b.uuid = $uuidOfBlobb " +
            "DELETE l")
    void unlikeBlobbWithUuid(UUID uuidOfUser, UUID uuidOfBlobb);

    // allow listing responses to blobbs marked as deleted
    // but dont list responses that are marked as deleted
    @Query( "MATCH (blobb:Blobb) WHERE blobb.uuid = $uuid " +
            "MATCH (u:User)-[:POSTS]->(response:ResponseBlobb)-[:RESPONDS]->(blobb) WHERE response.deleted <> true " +
            "RETURN response.uuid AS uuid, response.content AS content, response.creationDate AS date, u AS author, " +
            "NULL AS reblobbs, blobb.uuid AS respondsTo " +
            "ORDER BY date ASC SKIP $skip LIMIT $limit")
    List<FeedBlobb> getAllResponsesToBlobbWithUuid(UUID uuid, Long skip, Long limit);

    // allow listing reblobbs even when referenced blobb is marked as deleted
    // but don't list reblobbs that are marked as deleted
    @Query( "MATCH (blobb:Blobb) WHERE blobb.uuid = $uuid " +
            "MATCH (u:User)-[:POSTS]->(reblobb:Reblobb)-[:REBLOBBS]->(blobb) WHERE reblobb.deleted <> true " +
            "RETURN reblobb.uuid AS uuid, reblobb.content AS content, reblobb.creationDate AS date, u AS author, " +
            "blobb.uuid AS reblobbs, NULL AS respondsTo " +
            "ORDER BY date ASC SKIP $skip LIMIT $limit")
    List<FeedBlobb> getAllReblobbsOfBlobbWithUuid(UUID uuid, Long skip, Long limit);
}
