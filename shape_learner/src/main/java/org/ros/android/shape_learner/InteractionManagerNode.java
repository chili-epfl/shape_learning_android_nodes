/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.shape_learner;

import android.util.Log;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import java.util.ArrayList;

import geometry_msgs.PointStamped;
import geometry_msgs.PoseStamped;
import nav_msgs.Path;
import std_msgs.*;
import std_msgs.String;

/**
 * ROS node for capturing and publishing user interactions.
 *
 * @author Deanna Hood
 */

class InteractionManagerNode extends AbstractNodeMain {
    private static final java.lang.String TAG = "InteractionManager";
    private java.lang.String touchInfoTopicName;
    private ConnectedNode connectedNode;
    private Publisher<PointStamped> touchInfoPublisher;
    private Publisher<PointStamped> gestureInfoPublisher;
    private java.lang.String gestureInfoTopicName;
    private Publisher<Empty> clearScreenPublisher;
    private java.lang.String clearScreenTopicName;
    private Publisher<Path> userDrawnShapePublisher;
    private java.lang.String userDrawnShapeTopicName;
    private Publisher<Float32> timeResponsePublisher;
    private java.lang.String TimeResponseTopicName;
    private Publisher<Float32> timeWritingPublisher;
    private java.lang.String TimeWritingTopicName;
    private Publisher<std_msgs.String> userFeedbackPublisher;
    private java.lang.String FeedbackTopicName;

    public void setTouchInfoTopicName(java.lang.String topicName) {
        this.touchInfoTopicName = topicName;
    }
    public void setGestureInfoTopicName(java.lang.String topicName) { this.gestureInfoTopicName = topicName; }
    public void setClearScreenTopicName(java.lang.String topicName) {
        this.clearScreenTopicName = topicName;
    }
    public void setUserDrawnShapeTopicName(java.lang.String topicName) { this.userDrawnShapeTopicName = topicName; }
    public void setTimeResponseTopicName(java.lang.String topicName) {this.TimeResponseTopicName = topicName; }
    public void setTimeWritingTopicName(java.lang.String topicName) {this.TimeWritingTopicName = topicName; }
    public void setFeedbackTopicName(java.lang.String topicName) {this.FeedbackTopicName = topicName; }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("interaction_publisher");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        this.touchInfoPublisher =
                connectedNode.newPublisher(touchInfoTopicName, geometry_msgs.PointStamped._TYPE);
        this.touchInfoPublisher.setLatchMode(false);
        this.gestureInfoPublisher =
                connectedNode.newPublisher(gestureInfoTopicName, geometry_msgs.PointStamped._TYPE);
        this.gestureInfoPublisher.setLatchMode(false);
        this.clearScreenPublisher =
                connectedNode.newPublisher(clearScreenTopicName, Empty._TYPE);
        this.userDrawnShapePublisher =
                connectedNode.newPublisher(userDrawnShapeTopicName, Path._TYPE);
        this.timeResponsePublisher =
                connectedNode.newPublisher(TimeResponseTopicName, Float32._TYPE);
        this.timeWritingPublisher =
                connectedNode.newPublisher(TimeWritingTopicName, Float32._TYPE);
        this.userFeedbackPublisher =
                connectedNode.newPublisher(FeedbackTopicName, std_msgs.String._TYPE);

    }

    public void publishTouchInfoMessage(double x, double y) {

        geometry_msgs.PointStamped pointStamped = touchInfoPublisher.newMessage();
        pointStamped.getHeader().setStamp(connectedNode.getCurrentTime());
        pointStamped.getPoint().setX(x);
        pointStamped.getPoint().setY(y);

        touchInfoPublisher.publish(pointStamped);

    }
    public void publishGestureInfoMessage(double x, double y) {

        geometry_msgs.PointStamped pointStamped = gestureInfoPublisher.newMessage();
        pointStamped.getHeader().setStamp(connectedNode.getCurrentTime());
        pointStamped.getPoint().setX(x);
        pointStamped.getPoint().setY(y);

        gestureInfoPublisher.publish(pointStamped);

    }

    public void publishClearScreenMessage(){
        Log.e(TAG, "Publishing clear screen request");
        Empty message = clearScreenPublisher.newMessage();
        clearScreenPublisher.publish(message);
    }

    public void publishUserDrawnShapeMessage(ArrayList<double[]> points){
        nav_msgs.Path message = userDrawnShapePublisher.newMessage();
        message.getHeader().setStamp(connectedNode.getCurrentTime());

        for(double[] point : points){
            PoseStamped poseStamped = connectedNode.getTopicMessageFactory().newFromType(PoseStamped._TYPE);

            poseStamped.getPose().getPosition().setX(point[0]);
            poseStamped.getPose().getPosition().setY(point[1]);

            message.getPoses().add(poseStamped);
        }

        Log.e(TAG, "Publishing user-drawn shape");
        userDrawnShapePublisher.publish(message);
    }

    public void publishUserDrawnMessageMessage(ArrayList< ArrayList<double[]> > userDrawnMessage){
        Log.e(TAG, "Publishing user-drawn message");
        for(ArrayList<double[]> stroke : userDrawnMessage){
            publishUserDrawnShapeMessage(stroke);
        }
        publishUserDrawnShapeMessage(new ArrayList<double[]>()); //publish empty stroke to show message is done
    }

    public void publishUserFeedbackMessage(java.lang.String userFeedbackMessage){
        Log.e(TAG, "Publishing feedback message");
        std_msgs.String m = userFeedbackPublisher.newMessage();
        m.setData(userFeedbackMessage);
        userFeedbackPublisher.publish(m);
    }

    public void publishTimeResponse(long x) {

        Float32 time = timeResponsePublisher.newMessage();
        time.setData(x);
        timeResponsePublisher.publish(time);

    }

    public void publishTimeWriting(long x) {

        Float32 time = timeWritingPublisher.newMessage();
        time.setData(x);
        timeWritingPublisher.publish(time);

    }
}