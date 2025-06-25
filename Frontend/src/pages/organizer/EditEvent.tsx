import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Calendar, Users, Clock, FileText, Save } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import type { Event } from '../../types/database';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';
import { supabase } from '../../lib/supabase';

const EVENT_TAGS = ['Academic', 'Social', 'Sports', 'Career', 'Workshop', 'Cultural', 'Technology', 'Arts'];
const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

interface EventFormData {
  title: string;
  description: string;
  start_time: string;
  end_time: string;
  capacity: number;
  tag: string[];
}

export function EditEvent() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [currentRegistrations, setCurrentRegistrations] = useState(0);
  const [formData, setFormData] = useState<EventFormData>({
    title: '',
    description: '',
    start_time: '',
    end_time: '',
    capacity: 50,
    tag: [],
  });

  useEffect(() => {
    if (id && user) {
      fetchEvent();
    }
  }, [id, user]);

  const fetchEvent = async () => {
    try {
      setLoading(true);
      
     // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }

      // Fetch event details
      const eventResponse = await fetch(`${BACKEND_URL}/api/events/${id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`
        }
      });

      if (!eventResponse.ok) {
        throw new Error(`Failed to fetch event: ${eventResponse.status}`);
      }

      const eventData = await eventResponse.json();
      
      // Check if the current user is the host
      if (eventData.hostUuid !== user!.id) {
        throw new Error('Unauthorized: You are not the host of this event');
      }
      
      setEvent(eventData);
      setFormData({
        title: eventData.title,
        description: eventData.description || '',
        start_time: eventData.startTime,
        end_time: eventData.endTime,
        capacity: eventData.capacity || 50,
        tag: eventData.tag || [],
      });

      // Get current registration count
      const participationsResponse = await fetch(`${BACKEND_URL}/api/participates/count/event/${eventData.id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`
        }
      });
      
      if (participationsResponse.ok) {
        const count = await participationsResponse.json();
        setCurrentRegistrations(count);
      }
    } catch (error) {
      console.error('Error fetching event:', error);
      navigate('/organizer/dashboard');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'capacity' ? parseInt(value) || 0 : value
    }));
  };

  const handleTagToggle = (tag: string) => {
    setFormData(prev => ({
      ...prev,
      tag: prev.tag.includes(tag)
        ? prev.tag.filter((t: string) => t !== tag)
        : [...prev.tag, tag]
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!event) return;

    // Validation
    if (!formData.title || !formData.description || !formData.start_time || 
        !formData.end_time || formData.capacity <= 0 || formData.tag.length === 0) {
      alert('Please fill in all required fields.');
      return;
    }

    // Check if capacity is being reduced below current registrations
    if (formData.capacity < currentRegistrations) {
      alert(`Cannot reduce capacity below current registrations (${currentRegistrations})`);
      return;
    }

    try {
      setSaving(true);
      
      // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }
      
      // Create FormData for multipart request (even without files)
      const formDataToSend = new FormData();
      
      // Create event object matching your backend DTO structure
      const eventDto = {
        title: formData.title,
        description: formData.description,
        startTime: formData.start_time,
        endTime: formData.end_time,
        capacity: formData.capacity,
        tag: formData.tag
      };
      
      formDataToSend.append('event', JSON.stringify(eventDto));
      
      const response = await fetch(`${BACKEND_URL}/api/events/${event.id}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${session.access_token}`
          // Don't set Content-Type for FormData, let browser set it with boundary
        },
        body: formDataToSend
      });

      if (!response.ok) {
        throw new Error(`Failed to update event: ${response.status}`);
      }

      navigate('/organizer/dashboard');
    } catch (error) {
      console.error('Error updating event:', error);
      alert('Failed to update event. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!event) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Event not found</h1>
        <Link
          to="/organizer/dashboard"
          className="text-blue-600 hover:text-blue-800"
        >
          Return to Dashboard
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <Link
          to="/organizer/dashboard"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <ArrowLeft className="w-5 h-5 mr-2" />
          Back to Dashboard
        </Link>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Edit Event</h1>
        <p className="text-gray-600">Update your event details</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Basic Information */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Basic Information</h2>
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="lg:col-span-2">
              <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-2">
                Event Title *
              </label>
              <div className="relative">
                <FileText className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  id="title"
                  name="title"
                  required
                  value={formData.title}
                  onChange={handleInputChange}
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Enter event title"
                />
              </div>
            </div>

            <div className="lg:col-span-2">
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Event Description *
              </label>
              <textarea
                id="description"
                name="description"
                required
                rows={4}
                value={formData.description}
                onChange={handleInputChange}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Describe your event..."
              />
            </div>

            <div>
              <label htmlFor="start_time" className="block text-sm font-medium text-gray-700 mb-2">
                Start Time *
              </label>
              <div className="relative">
                <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="datetime-local"
                  id="start_time"
                  name="start_time"
                  required
                  value={formData.start_t_time.slice(0, 16)}
                  onChange={handleInputChange}
                  min={new Date().toISOString().slice(0, 16)}
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <label htmlFor="end_time" className="block text-sm font-medium text-gray-700 mb-2">
                End Time *
              </label>
              <div className="relative">
                <Clock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="datetime-local"
                  id="end_time"
                  name="end_time"
                  required
                  value={formData.end_time.slice(0, 16)}
                  onChange={handleInputChange}
                  min={formData.start_time.slice(0, 16)}
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div className="lg:col-span-2">
              <label htmlFor="capacity" className="block text-sm font-medium text-gray-700 mb-2">
                Capacity * (Current: {currentRegistrations} registered)
              </label>
              <div className="relative">
                <Users className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="number"
                  id="capacity"
                  name="capacity"
                  required
                  min={currentRegistrations}
                  value={formData.capacity}
                  onChange={handleInputChange}
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Maximum attendees"
                />
              </div>
              <p className="text-sm text-gray-500 mt-1">
                Cannot be reduced below current registrations ({currentRegistrations})
              </p>
            </div>
          </div>
        </div>

        {/* Tags */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Event Tags *</h2>
          <p className="text-gray-600 mb-4">Select one or more tags that best describe your event</p>
          
          <div className="flex flex-wrap gap-3">
            {EVENT_TAGS.map(tag => (
              <button
                key={tag}
                type="button"
                onClick={() => handleTagToggle(tag)}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200 ${
                  formData.tag.includes(tag)
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {tag}
              </button>
            ))}
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex justify-end space-x-4">
          <Link
            to="/organizer/dashboard"
            className="px-6 py-3 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors duration-200"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={saving}
            className="flex items-center space-x-2 px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saving ? (
              <>
                <LoadingSpinner size="sm" className="text-white" />
                <span>Saving...</span>
              </>
            ) : (
              <>
                <Save className="w-5 h-5" />
                <span>Save Changes</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
