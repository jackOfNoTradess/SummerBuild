import { useState, useEffect } from 'react';
import { Search, Filter, Calendar as CalendarIcon, Users, Clock } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import { Link } from 'react-router-dom';
import { supabase } from '../lib/supabase';
import type { ApiEventResponse } from '../types/database';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

const EVENT_TAGS = ['Academic', 'Social', 'Sports', 'Career', 'Workshop', 'Cultural', 'Technology', 'Arts'];
const TAG_COLORS: Record<string, string> = {
  Academic: 'bg-blue-100 text-blue-800',
  Social: 'bg-green-100 text-green-800',
  Sports: 'bg-orange-100 text-orange-800',
  Career: 'bg-purple-100 text-purple-800',
  Workshop: 'bg-indigo-100 text-indigo-800',
  Cultural: 'bg-pink-100 text-pink-800',
  Technology: 'bg-gray-100 text-gray-800',
  Arts: 'bg-yellow-100 text-yellow-800',
};

interface EventWithParticipationCount extends ApiEventResponse {
  participationCount: number;
}

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

export function Dashboard() {
  const [events, setEvents] = useState<EventWithParticipationCount[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<'date' | 'popularity'>('date');

  useEffect(() => {
    fetchEvents();
  }, []);

  const fetchEvents = async () => {
    try {
      setLoading(true);
      
      // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }
      
      // Fetch events from backend
      const eventsResponse = await fetch(`${BACKEND_URL}/api/events`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`,
        },
      });

      if (!eventsResponse.ok) {
        throw new Error(`HTTP error! status: ${eventsResponse.status}`);
      }

      const eventsData = await eventsResponse.json();

      // // Filter events to only show future events
      // const futureEvents = eventsData.filter((event: Event) => 
      //   new Date(event.start_time) >= new Date()
      // );

      // Sort events by start time
      const sortedEvents = eventsData.sort((a: ApiEventResponse, b: ApiEventResponse) => 
        new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
      );

      // Fetch participation counts for each event
      const eventsWithCounts = await Promise.all(
        sortedEvents.map(async (event: ApiEventResponse) => {
          try {
            const countResponse = await fetch(`${BACKEND_URL}/api/participates/count/event/${event.id}`, {
              method: 'GET',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${session.access_token}`,
              },
            });

            let participationCount = 0;
            if (countResponse.ok) {
              participationCount = await countResponse.json();
            } else {
              console.warn(`Failed to fetch participation count for event ${event.id}`);
            }

            return {
              ...event,
              participationCount
            };
          } catch (error) {
            console.error(`Error fetching participation count for event ${event.id}:`, error);
            return {
              ...event,
              participationCount: 0
            };
          }
        })
      );

      setEvents(eventsWithCounts);
    } catch (error) {
      console.error('Error fetching events:', error);
      setEvents([]);
    } finally {
      setLoading(false);
    }
  };

  const filteredEvents = events.filter(event => {
    const matchesSearch = event.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         (event.description && event.description.toLowerCase().includes(searchQuery.toLowerCase()));
    
    const matchesTags = selectedTags.length === 0 || 
                       (event.tags && event.tags.length > 0 && selectedTags.some(tag => event.tags?.includes(tag)));
    
    return matchesSearch && matchesTags;
  }).sort((a, b) => {
    if (sortBy === 'date') {
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
    } else {
      return b.participationCount - a.participationCount;
    }
  });

  const toggleTag = (tag: string) => {
    setSelectedTags(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Discover Campus Events
        </h1>
        <p className="text-xl text-gray-600 max-w-2xl mx-auto">
          Find and join exciting events happening on campus. From academic workshops to social gatherings, there's something for everyone.
        </p>
      </div>

      {/* Search and Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col lg:flex-row gap-4 mb-6">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="Search events..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Sort */}
          <div className="flex items-center space-x-2">
            <Filter className="w-5 h-5 text-gray-400" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as 'date' | 'popularity')}
              className="border border-gray-300 rounded-lg px-3 py-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="date">Sort by Date</option>
              <option value="popularity">Sort by Popularity</option>
            </select>
          </div>
        </div>

        {/* Tag Filters */}
        <div className="flex flex-wrap gap-2">
          {EVENT_TAGS.map(tag => (
            <button
              key={tag}
              onClick={() => toggleTag(tag)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200 ${
                selectedTags.includes(tag)
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {tag}
            </button>
          ))}
        </div>
      </div>

      {/* Events Grid */}
      {filteredEvents.length === 0 ? (
        <div className="text-center py-12">
          <CalendarIcon className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No events found</h3>
          <p className="text-gray-600">Try adjusting your search or filters to find more events.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredEvents.map(event => (
            <Link
              key={event.id}
              to={`/events/${event.id}`}
              className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow duration-300"
            >
              {/* Event Image */}
              <div className="h-48 bg-gradient-to-r from-blue-500 to-purple-600 relative">
                <div className="w-full h-full flex items-center justify-center">
                  <CalendarIcon className="w-16 h-16 text-white/80" />
                </div>
                {/* Availability Badge */}
                <div className="absolute top-4 right-4">
                  {event.capacity && event.participationCount >= event.capacity ? (
                    <span className="bg-red-100 text-red-800 text-xs font-medium px-2 py-1 rounded-full">
                      Fully Booked
                    </span>
                  ) : (
                    <span className="bg-green-100 text-green-800 text-xs font-medium px-2 py-1 rounded-full">
                      Available
                    </span>
                  )}
                </div>
              </div>

              {/* Event Content */}
              <div className="p-6">
                {/* Tags */}
                {event.tags && event.tags.length > 0 && (
                  <div className="flex flex-wrap gap-1 mb-3">
                    {event.tags.slice(0, 2).map((tag, index) => (
                      <span
                        key={index}
                        className={`text-xs font-medium px-2 py-1 rounded-full ${TAG_COLORS[tag] || 'bg-gray-100 text-gray-800'}`}
                      >
                        {tag}
                      </span>
                    ))}
                    {event.tags.length > 2 && (
                      <span className="text-xs font-medium px-2 py-1 rounded-full bg-gray-100 text-gray-800">
                        +{event.tags.length - 2} more
                      </span>
                    )}
                  </div>
                )}

                {/* Title */}
                <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                  {event.title}
                </h3>

                {/* Description */}
                {event.description && (
                  <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                    {event.description}
                  </p>
                )}

                {/* Event Details */}
                <div className="space-y-2 text-sm text-gray-500">
                  <div className="flex items-center space-x-2">
                    <CalendarIcon className="w-4 h-4" />
                    <span>{format(parseISO(event.startTime), 'MMM d, yyyy')}</span>
                    <Clock className="w-4 h-4 ml-2" />
                    <span>{format(parseISO(event.startTime), 'h:mm a')}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Users className="w-4 h-4" />
                    <span>{event.participationCount} / {event.capacity || 'Unlimited'} registered</span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
