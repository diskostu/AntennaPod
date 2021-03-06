package de.danoeh.antennapod.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.comparator.SearchResultValueComparator;

/** Performs search on Feeds and FeedItems */
public class FeedSearcher {
	private static final String TAG = "FeedSearcher";

	// Search result values
	private static final int VALUE_FEED_TITLE = 3;
	private static final int VALUE_ITEM_TITLE = 2;
	private static final int VALUE_ITEM_CHAPTER = 1;
	private static final int VALUE_ITEM_DESCRIPTION = 0;
	private static final int VALUE_WORD_MATCH = 4;

	/** Performs a search in all feeds or one specific feed. */
	public static ArrayList<SearchResult> performSearch(final String query,
			Feed selectedFeed) {
		String lcQuery = query.toLowerCase();
		ArrayList<SearchResult> result = new ArrayList<SearchResult>();
		if (selectedFeed == null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Performing global search");
			if (AppConfig.DEBUG)
				Log.d(TAG, "Searching Feed titles");
			searchFeedtitles(lcQuery, result);
		} else if (AppConfig.DEBUG) {
			Log.d(TAG, "Performing search on specific feed");
		}

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching Feeditem titles");
		searchFeedItemTitles(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching item-chaptertitles");
		searchFeedItemChapters(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching item descriptions");
		searchFeedItemDescription(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching item content encoded data");
		searchFeedItemContentEncoded(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Sorting results");
		Collections.sort(result, new SearchResultValueComparator());

		return result;
	}

	private static void searchFeedtitles(String query,
			ArrayList<SearchResult> destination) {
		FeedManager manager = FeedManager.getInstance();
		for (Feed feed : manager.getFeeds()) {
			SearchResult result = createSearchResult(feed, query, feed
					.getTitle().toLowerCase(), VALUE_FEED_TITLE);
			if (result != null) {
				destination.add(result);
			}
		}
	}

	private static void searchFeedItemTitles(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemTitlesSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemTitlesSingleFeed(query, destination, selectedFeed);
		}
	}

	private static void searchFeedItemTitlesSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			SearchResult result = createSearchResult(item, query, item
					.getTitle().toLowerCase(), VALUE_ITEM_TITLE);
			if (result != null) {
				result.setSubtitle(PodcastApp.getInstance().getString(
						R.string.found_in_title_label));
				destination.add(result);
			}

		}
	}

	private static void searchFeedItemChapters(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemChaptersSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemChaptersSingleFeed(query, destination, selectedFeed);
		}
	}

	private static void searchFeedItemChaptersSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getChapters() != null) {
				for (Chapter sc : item.getChapters()) {
					SearchResult result = createSearchResult(item, query, sc
							.getTitle().toLowerCase(), VALUE_ITEM_CHAPTER);
					if (result != null) {
						result.setSubtitle(PodcastApp.getInstance().getString(
								R.string.found_in_chapters_label));
						destination.add(result);
					}
				}
			}
		}
	}

	private static void searchFeedItemDescription(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemDescriptionSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemDescriptionSingleFeed(query, destination,
					selectedFeed);
		}
	}

	private static void searchFeedItemDescriptionSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getDescription() != null) {
				SearchResult result = createSearchResult(item, query, item
						.getDescription().toLowerCase(), VALUE_ITEM_DESCRIPTION);
				if (result != null) {
					result.setSubtitle(PodcastApp.getInstance().getString(
							R.string.found_in_shownotes_label));
					destination.add(result);
				}

			}
		}
	}

	private static void searchFeedItemContentEncoded(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemContentEncodedSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemContentEncodedSingleFeed(query, destination,
					selectedFeed);
		}
	}

	private static void searchFeedItemContentEncodedSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (!destination.contains(item) && item.getContentEncoded() != null) {
				SearchResult result = createSearchResult(item, query, item
						.getContentEncoded().toLowerCase(),
						VALUE_ITEM_DESCRIPTION);
				if (result != null) {
					result.setSubtitle(PodcastApp.getInstance().getString(
							R.string.found_in_shownotes_label));
					destination.add(result);
				}
			}
		}
	}

	private static SearchResult createSearchResult(FeedComponent component,
			String query, String text, int baseValue) {
		int bonus = 0;
		boolean found = false;
		// try word search
		Pattern word = Pattern.compile("\b" + query + "\b");
		Matcher matcher = word.matcher(text);
		found = matcher.find();
		if (found) {
			bonus = VALUE_WORD_MATCH;
		} else {
			// search for other occurence
			found = text.contains(query);
		}

		if (found) {
			return new SearchResult(component, baseValue + bonus);
		} else {
			return null;
		}
	}

}
