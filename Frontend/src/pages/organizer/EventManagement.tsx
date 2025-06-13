import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Users, Download, Mail, QrCode, Calendar, MapPin, TrendingUp, CheckCircle } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import { supabase } from '../../lib/supabase';
import { useAuth } from '../../contexts/AuthContext';
import { Event, Booking, Profile } from '../../types/database';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';

interface BookingWithProfile extends Booking {
  profiles: Profile;
}

export function EventManagement() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [event, setEvent] = useState<Event | null>(null);
  const [bookings, setBookings] = useState<BookingWithProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'attendees' | 'analytics'>('overview');

  useEffect(() => {
    if (id && user) {
      fetchEventData();
    }
  }, [id, user]);

  const fetchEventData = async () => {
    try {
      setLoading(true);
      
      // Fetch event details
      const { data: eventData, error: eventError } = await supabase
        .from('events')
        .select('*')
        .eq('id', id)
        .eq('organizer_id', user!.id)
        .single();

      if (eventError) throw eventError;
      setEvent(eventData);

      // Fetch bookings with user profiles
      const { data: bookingsData, error: bookingsError } = await supabase
        .from('bookings')
        .select(`
          *,
          profiles:user_id (*)
        `)
        .eq('event_id', id)
        .eq('status', 'confirmed')
        .order('created_at', { ascending: false });

      if (bookingsError) throw bookingsError;
      setBookings(bookingsData as BookingWithProfile[] || []);
    } catch (error) {
      console.error('Error fetching event data:', error);
    } finally {
      setLoading(false);
    }
  };

  const exportAttendees = () => {
    if (bookings.length === 0) return;

    const csvContent = [
      ['Name', 'Email', 'Booking Reference', 'Booking Date', 'Tickets'],
      ...bookings.map(booking => [
        booking.profiles.full_name,
        booking.profiles.email,
        booking.booking_reference,
        format(parseISO(booking.created_at), 'yyyy-MM-dd HH:mm'),
        booking.tickets_quantity.toString()
      ])
    ].map(row => row.join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${event?.title}-attendees.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
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

  const attendanceRate = (event.registered_count / event.capacity) * 100;
  const isUpcoming = new Date(event.event_date) >= new Date();

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <Link
          to="/organizer/dashboard"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <ArrowLeft className="w-5 h-5 mr-2" />
          Back to Dashboard
        </Link>
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">{event.title}</h1>
            <p className="text-gray-600">Manage your event and track attendees</p>
          </div>
          <div className="flex items-center space-x-3">
            <Link
              to={`/organizer/events/${event.id}/edit`}
              className="px-4 py-2 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors duration-200"
            >
              Edit Event
            </Link>
            <button
              onClick={exportAttendees}
              disabled={bookings.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Download className="w-4 h-4" />
              <span>Export Attendees</span>
            </button>
          </div>
        </div>
      </div>

      {/* Event Summary */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-600 mb-2">{event.registered_count}</div>
            <div className="text-sm text-gray-600">Total Registrations</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-green-600 mb-2">{event.capacity}</div>
            <div className="text-sm text-gray-600">Event Capacity</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-purple-600 mb-2">{attendanceRate.toFixed(0)}%</div>
            <div className="text-sm text-gray-600">Attendance Rate</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-orange-600 mb-2">{event.capacity - event.registered_count}</div>
            <div className="text-sm text-gray-600">Available Spots</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-8 pt-6">
            {[
              { id: 'overview', label: 'Overview', icon: Calendar },
              { id: 'attendees', label: 'Attendees', icon: Users },
              { id: 'analytics', label: 'Analytics', icon: TrendingUp },
            ].map(tab => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as typeof activeTab)}
                  className={`flex items-center space-x-2 pb-4 px-1 border-b-2 font-medium text-sm ${
                    activeTab === tab.id
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="p-8">
          {activeTab === 'overview' && (
            <div className="space-y-8">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Event Details</h3>
                  <div className="space-y-4">
                    <div className="flex items-start space-x-3">
                      <Calendar className="w-5 h-5 text-blue-600 mt-1" />
                      <div>
                        <div className="font-medium text-gray-900">
                          {format(parseISO(event.event_date), 'EEEE, MMMM d, yyyy')}
                        </div>
                        <div className="text-sm text-gray-500">{event.event_time}</div>
                      </div>
                    </div>
                    <div className="flex items-start space-x-3">
                      <MapPin className="w-5 h-5 text-blue-600 mt-1" />
                      <div>
                        <div className="font-medium text-gray-900">Location</div>
                        <div className="text-sm text-gray-500">{event.location}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Registration Progress</h3>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-600">Progress</span>
                      <span className="text-gray-900 font-medium">{attendanceRate.toFixed(1)}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-3">
                      <div
                        className="bg-blue-600 h-3 rounded-full transition-all duration-300"
                        style={{ width: `${attendanceRate}%` }}
                      ></div>
                    </div>
                    <div className="flex items-center justify-between text-sm text-gray-600">
                      <span>{event.registered_count} registered</span>
                      <span>{event.capacity - event.registered_count} remaining</span>
                    </div>
                  </div>
                </div>
              </div>

              {event.additional_details && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Additional Information</h3>
                  <div className="bg-gray-50 rounded-lg p-6">
                    <p className="text-gray-700 leading-relaxed">{event.additional_details}</p>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'attendees' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">
                  Registered Attendees ({bookings.length})
                </h3>
                <button
                  onClick={exportAttendees}
                  disabled={bookings.length === 0}
                  className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Download className="w-4 h-4" />
                  <span>Export CSV</span>
                </button>
              </div>

              {bookings.length === 0 ? (
                <div className="text-center py-12">
                  <Users className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                  <h4 className="text-lg font-semibold text-gray-900 mb-2">No attendees yet</h4>
                  <p className="text-gray-600">Registrations will appear here once people start booking.</p>
                </div>
              ) : (
                <div className="overflow-hidden border border-gray-200 rounded-lg">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Attendee
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Booking Reference
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Booking Date
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Tickets
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Status
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {bookings.map(booking => (
                        <tr key={booking.id} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center">
                              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                                <span className="text-sm font-medium text-blue-600">
                                  {booking.profiles.full_name.charAt(0)}
                                </span>
                              </div>
                              <div className="ml-4">
                                <div className="text-sm font-medium text-gray-900">{booking.profiles.full_name}</div>
                                <div className="text-sm text-gray-500">{booking.profiles.email}</div>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm font-mono text-gray-900">{booking.booking_reference}</div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">
                              {format(parseISO(booking.created_at), 'MMM d, yyyy')}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">{booking.tickets_quantity}</div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                              <CheckCircle className="w-3 h-3 mr-1" />
                              Confirmed
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === 'analytics' && (
            <div className="space-y-8">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="bg-gray-50 rounded-lg p-6">
                  <h4 className="text-lg font-semibold text-gray-900 mb-4">Registration Timeline</h4>
                  <div className="space-y-4">
                    {bookings.length === 0 ? (
                      <p className="text-gray-600">No registrations yet</p>
                    ) : (
                      <div className="text-center">
                        <div className="text-2xl font-bold text-blue-600 mb-2">
                          {bookings.length}
                        </div>
                        <div className="text-sm text-gray-600">Total Registrations</div>
                      </div>
                    )}
                  </div>
                </div>

                <div className="bg-gray-50 rounded-lg p-6">
                  <h4 className="text-lg font-semibold text-gray-900 mb-4">Event Status</h4>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Event Type</span>
                      <span className="font-medium text-gray-900">
                        {isUpcoming ? 'Upcoming' : 'Past'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Registration</span>
                      <span className={`font-medium ${
                        new Date() <= new Date(event.registration_deadline) 
                          ? 'text-green-600' 
                          : 'text-red-600'
                      }`}>
                        {new Date() <= new Date(event.registration_deadline) ? 'Open' : 'Closed'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Capacity Status</span>
                      <span className={`font-medium ${
                        event.registered_count >= event.capacity 
                          ? 'text-red-600' 
                          : 'text-green-600'
                      }`}>
                        {event.registered_count >= event.capacity ? 'Full' : 'Available'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-4">Event Performance</h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                  <div className="text-center">
                    <div className="text-xl font-bold text-blue-600 mb-1">
                      {attendanceRate.toFixed(1)}%
                    </div>
                    <div className="text-sm text-gray-600">Fill Rate</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-green-600 mb-1">
                      {bookings.reduce((sum, b) => sum + b.tickets_quantity, 0)}
                    </div>
                    <div className="text-sm text-gray-600">Total Tickets</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-purple-600 mb-1">
                      {Math.round(bookings.reduce((sum, b) => sum + b.tickets_quantity, 0) / Math.max(bookings.length, 1) * 10) / 10}
                    </div>
                    <div className="text-sm text-gray-600">Avg Tickets/Person</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-orange-600 mb-1">
                      {event.capacity - event.registered_count}
                    </div>
                    <div className="text-sm text-gray-600">Remaining Spots</div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}