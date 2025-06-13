import React, { useState, useEffect } from 'react';
import { Search, Filter, Calendar as CalendarIcon, MapPin, Users, Clock, Tag } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import { Link } from 'react-router-dom';
import { supabase } from '../lib/supabase';
import { Event } from '../types/database';
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

export function Dashboard() {
  const [events, setEvents] = useState<Event[]>([]);
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
      const { data, error } = await supabase
        .from('events')
        .select('*')
        .gte('event_date', new Date().toISOString().split('T')[0])
        .order('event_date', { ascending: true });

      if (error) throw error;
      setEvents(data || []);
    } catch (error) {
      console.error('Error fetching events:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredEvents = events.filter(event => {
    const matchesSearch = event.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         event.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         event.location.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesTags = selectedTags.length === 0 || 
                       selectedTags.some(tag => event.tags.includes(tag));
    
    return matchesSearch && matchesTags;
  }).sort((a, b) => {
    if (sortBy === 'date') {
      return new Date(a.event_date).getTime() - new Date(b.event_date).getTime();
    } else {
      return b.registered_count - a.registered_count;
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
                {event.image_url ? (
                  <img
                    src={event.image_url}
                    alt={event.title}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <CalendarIcon className="w-16 h-16 text-white/80" />
                  </div>
                )}
                {/* Availability Badge */}
                <div className="absolute top-4 right-4">
                  {event.registered_count >= event.capacity ? (
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
                <div className="flex flex-wrap gap-1 mb-3">
                  {event.tags.slice(0, 2).map(tag => (
                    <span
                      key={tag}
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

                {/* Title */}
                <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                  {event.title}
                </h3>

                {/* Description */}
                <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                  {event.description}
                </p>

                {/* Event Details */}
                <div className="space-y-2 text-sm text-gray-500">
                  <div className="flex items-center space-x-2">
                    <CalendarIcon className="w-4 h-4" />
                    <span>{format(parseISO(event.event_date), 'MMM d, yyyy')}</span>
                    <Clock className="w-4 h-4 ml-2" />
                    <span>{event.event_time}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <MapPin className="w-4 h-4" />
                    <span className="truncate">{event.location}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Users className="w-4 h-4" />
                    <span>{event.registered_count} / {event.capacity} registered</span>
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