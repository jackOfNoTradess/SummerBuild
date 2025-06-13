import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Calendar, MapPin, Users, Clock, User, ArrowLeft, ExternalLink } from 'lucide-react';
import { format, parseISO, isBefore } from 'date-fns';
import { supabase } from '../lib/supabase';
import { useAuth } from '../contexts/AuthContext';
import { Event, Booking } from '../types/database';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

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

export function EventDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, profile } = useAuth();
  const [event, setEvent] = useState<Event | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (id) {
      fetchEventDetails();
    }
  }, [id, user]);

  const fetchEventDetails = async () => {
    try {
      setLoading(true);
      
      // Fetch event details
      const { data: eventData, error: eventError } = await supabase
        .from('events')
        .select('*')
        .eq('id', id)
        .single();

      if (eventError) throw eventError;
      setEvent(eventData);

      // Check if user has booked this event
      if (user) {
        const { data: bookingData, error: bookingError } = await supabase
          .from('bookings')
          .select('*')
          .eq('event_id', id)
          .eq('user_id', user.id)
          .eq('status', 'confirmed')
          .maybeSingle();

        if (bookingError && bookingError.code !== 'PGRST116') {
          throw bookingError;
        }
        
        setBooking(bookingData);
      }
    } catch (error) {
      console.error('Error fetching event details:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleBookEvent = async () => {
    if (!user || !event) return;

    try {
      setActionLoading(true);
      
      // Generate booking reference
      const bookingRef = `BK${Date.now()}${Math.random().toString(36).substr(2, 4).toUpperCase()}`;
      
      // Create booking
      const { error: bookingError } = await supabase
        .from('bookings')
        .insert({
          event_id: event.id,
          user_id: user.id,
          booking_reference: bookingRef,
          tickets_quantity: 1,
          status: 'confirmed'
        });

      if (bookingError) throw bookingError;

      // Update event registration count
      const { error: updateError } = await supabase
        .from('events')
        .update({ registered_count: event.registered_count + 1 })
        .eq('id', event.id);

      if (updateError) throw updateError;

      // Refresh data
      await fetchEventDetails();
      
      // Navigate to booking confirmation
      navigate(`/booking-confirmation/${bookingRef}`);
    } catch (error) {
      console.error('Error booking event:', error);
      alert('Failed to book event. Please try again.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelBooking = async () => {
    if (!booking || !event) return;

    const confirmed = window.confirm('Are you sure you want to cancel your booking?');
    if (!confirmed) return;

    try {
      setActionLoading(true);
      
      // Update booking status
      const { error: bookingError } = await supabase
        .from('bookings')
        .update({ status: 'cancelled' })
        .eq('id', booking.id);

      if (bookingError) throw bookingError;

      // Update event registration count
      const { error: updateError } = await supabase
        .from('events')
        .update({ registered_count: Math.max(0, event.registered_count - 1) })
        .eq('id', event.id);

      if (updateError) throw updateError;

      // Refresh data
      await fetchEventDetails();
    } catch (error) {
      console.error('Error cancelling booking:', error);
      alert('Failed to cancel booking. Please try again.');
    } finally {
      setActionLoading(false);
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
      <div className="text-center py-12">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Event not found</h1>
        <button
          onClick={() => navigate('/dashboard')}
          className="text-blue-600 hover:text-blue-800"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const isEventFull = event.registered_count >= event.capacity;
  const isRegistrationClosed = isBefore(parseISO(event.registration_deadline), new Date());
  const canBook = !isEventFull && !isRegistrationClosed && !booking;

  return (
    <div className="max-w-4xl mx-auto">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-5 h-5" />
        <span>Back</span>
      </button>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {/* Event Header Image */}
        <div className="h-64 md:h-80 bg-gradient-to-r from-blue-500 to-purple-600 relative">
          {event.image_url ? (
            <img
              src={event.image_url}
              alt={event.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <Calendar className="w-24 h-24 text-white/80" />
            </div>
          )}
          {/* Status Badge */}
          <div className="absolute top-6 right-6">
            {isEventFull ? (
              <span className="bg-red-100 text-red-800 text-sm font-medium px-3 py-1 rounded-full border border-red-200">
                Fully Booked
              </span>
            ) : booking ? (
              <span className="bg-green-100 text-green-800 text-sm font-medium px-3 py-1 rounded-full border border-green-200">
                You're Registered
              </span>
            ) : (
              <span className="bg-blue-100 text-blue-800 text-sm font-medium px-3 py-1 rounded-full border border-blue-200">
                Available
              </span>
            )}
          </div>
        </div>

        <div className="p-8">
          {/* Tags */}
          <div className="flex flex-wrap gap-2 mb-4">
            {event.tags.map(tag => (
              <span
                key={tag}
                className={`text-sm font-medium px-3 py-1 rounded-full ${TAG_COLORS[tag] || 'bg-gray-100 text-gray-800'}`}
              >
                {tag}
              </span>
            ))}
          </div>

          {/* Title and Description */}
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{event.title}</h1>
          <p className="text-lg text-gray-600 mb-8 leading-relaxed">{event.description}</p>

          {/* Event Details Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
            <div className="space-y-6">
              <div className="flex items-start space-x-3">
                <Calendar className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Date & Time</h3>
                  <p className="text-gray-600">
                    {format(parseISO(event.event_date), 'EEEE, MMMM d, yyyy')}
                  </p>
                  <p className="text-gray-600">{event.event_time}</p>
                </div>
              </div>

              <div className="flex items-start space-x-3">
                <MapPin className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Location</h3>
                  <p className="text-gray-600">{event.location}</p>
                </div>
              </div>
            </div>

            <div className="space-y-6">
              <div className="flex items-start space-x-3">
                <Users className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Capacity</h3>
                  <p className="text-gray-600">
                    {event.registered_count} / {event.capacity} registered
                  </p>
                  <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                    <div
                      className="bg-blue-600 h-2 rounded-full"
                      style={{ width: `${(event.registered_count / event.capacity) * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>

              <div className="flex items-start space-x-3">
                <Clock className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Registration Deadline</h3>
                  <p className="text-gray-600">
                    {format(parseISO(event.registration_deadline), 'MMM d, yyyy')}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Additional Details */}
          {event.additional_details && (
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Additional Information</h3>
              <div className="bg-gray-50 rounded-lg p-6">
                <p className="text-gray-700 leading-relaxed">{event.additional_details}</p>
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row gap-4">
            {booking ? (
              <button
                onClick={handleCancelBooking}
                disabled={actionLoading}
                className="flex-1 bg-red-600 text-white py-4 px-6 rounded-lg font-semibold hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? (
                  <div className="flex items-center justify-center space-x-2">
                    <LoadingSpinner size="sm" className="text-white" />
                    <span>Cancelling...</span>
                  </div>
                ) : (
                  'Cancel Registration'
                )}
              </button>
            ) : canBook ? (
              <button
                onClick={handleBookEvent}
                disabled={actionLoading}
                className="flex-1 bg-blue-600 text-white py-4 px-6 rounded-lg font-semibold hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? (
                  <div className="flex items-center justify-center space-x-2">
                    <LoadingSpinner size="sm" className="text-white" />
                    <span>Booking...</span>
                  </div>
                ) : (
                  'Book Now'
                )}
              </button>
            ) : (
              <button
                disabled
                className="flex-1 bg-gray-400 text-white py-4 px-6 rounded-lg font-semibold cursor-not-allowed"
              >
                {isEventFull ? 'Event Full' : 'Registration Closed'}
              </button>
            )}
          </div>

          {/* Booking Reference */}
          {booking && (
            <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg">
              <div className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                <span className="text-sm font-medium text-green-800">
                  Booking Reference: {booking.booking_reference}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}