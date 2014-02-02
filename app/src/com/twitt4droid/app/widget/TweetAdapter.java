/*
 * Copyright 2014 Daniel Pedraza-Arcega
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitt4droid.app.widget;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.twitt4droid.app.R;
import com.twitt4droid.app.task.ImageLoadingTask;

import twitter4j.Status;

import java.util.List;

public class TweetAdapter extends ArrayAdapter<Status> {

    public TweetAdapter(Context context, int resource) {
        super(context, resource);
    }
    
    public TweetAdapter(Context context, int resource, List<Status> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Status rowItem = getItem(position);
        LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tweet_list_item, null);
            holder = new ViewHolder()
                    .setContext(getContext())
                    .setProfileImage((ImageView) convertView.findViewById(R.id.profile_image_view))
                    .setTweetTextView((TextView) convertView.findViewById(R.id.tweet_content_text_view))
                    .setUsernameTextView((TextView) convertView.findViewById(R.id.username_text_view));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.setContent(rowItem);
        return convertView;
    }
    
    private static class ViewHolder {
        private Context context;
        private ImageView profileImage;
        private TextView usernameTextView;
        private TextView tweetTextView;

        public ViewHolder setContext(Context context) {
            this.context = context;
            return this;
        }

        public void setContent(Status status) {
            usernameTextView.setText(context.getString(R.string.tweet_username_format, status.getUser().getScreenName(), status.getUser().getName()));
            tweetTextView.setText(status.getText());
            new ImageLoadingTask()
                .setImageView(profileImage)
                .setLoadingResourceImageId(R.drawable.twitt4droid_no_profile_image)
                .execute(status.getUser().getProfileImageURL());
        }

        public ViewHolder setProfileImage(ImageView profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public ViewHolder setUsernameTextView(TextView usernameTextView) {
            this.usernameTextView = usernameTextView;
            return this;
        }
        
        public ViewHolder setTweetTextView(TextView tweetTextView) {
            this.tweetTextView = tweetTextView;
            return this;
        }
    }
}