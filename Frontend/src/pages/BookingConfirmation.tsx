import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { CheckCircle, Calendar, MapPin, Clock, User, Download, Mail, QrCode } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import QRCode from 'qrcode';
import { supabase } from '../lib/supabase';
import { Booking, Event, Profile } from '../types/database';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

export function BookingConfirmation() {
  const { bookingRef } = useParams<{ bookingRef: string }>();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [event, setEvent] = useState<Event | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [qrCodeUrl, setQrCodeUrl] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (bookingRef) {
      fetchBookingDetails();
    }
  }, [bookingRef]);

  const fetchBookingDetails = async () => {
    try {
      setLoading(true);
      
      // Fetch booking details
      const { data: bookingData, error: bookingError } = await supabase
        .from('bookings')
        .select('*')
        .eq('booking_reference', bookingRef)
        .single();

      if (bookingError) throw bookingError;
      setBooking(bookingData);

      // Fetch event details
      const { data: eventData, error: eventError } = await supabase
        .from('events')
        .select('*')
        .eq('id', bookingData.event_id)
        .single();

      if (eventError) throw eventError;
      setEvent(eventData);

      // Fetch user profile
      const { data: profileData, error: profileError } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', bookingData.user_id)
        .single();

      if (profileError) throw profileError;
      setProfile(profileData);

      // Generate QR code
      const qrData = JSON.stringify({
        bookingRef: bookingData.booking_reference,
        eventId: bookingData.event_id,
        userId: bookingData.user_id,
        eventTitle: eventData.title,
      });
      
      const qrCodeDataUrl = await QRCode.toDataURL(qrData, {
        width: 256,
        margin: 2,
        color: {
          dark: '#1f2937',
          light: '#ffffff',
        },
      });
      setQrCodeUrl(qrCodeDataUrl);

    } catch (error: any) {
      console.error('Error fetching booking details:', error);
      setError('Booking not found or invalid booking reference.');
    } finally {
      setLoading(false);
    }
  };

  const downloadQRCode = () => {
    if (!qrCodeUrl) return;
    
    const link = document.createElement('a');
    link.download = `event-ticket-${bookingRef}.png`;
    link.href = qrCodeUrl;
    link.click();
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !booking || !event || !profile) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="bg-red-50 border border-red-200 rounded-lg p-8">
          <h1 className="text-2xl font-bold text-red-900 mb-4">Booking Not Found</h1>
          <p className="text-red-700 mb-6">{error || 'The booking reference you provided is invalid or has expired.'}</p>
          <Link
            to="/dashboard"
            className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
          >
            Back to Events
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* Success Header */}
      <div className="text-center">
        <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
          <CheckCircle className="w-8 h-8 text-green-600" />
        </div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Booking Confirmed!</h1>
        <p className="text-gray-600">Your event registration has been successfully processed.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Booking Details */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Booking Details</h2>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Booking Reference</span>
              <span className="font-mono font-semibold text-gray-900">{booking.booking_reference}</span>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Status</span>
              <span className="bg-green-100 text-green-800 text-sm font-medium px-3 py-1 rounded-full">
                Confirmed
              </span>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Tickets</span>
              <span className="font-semibold text-gray-900">{booking.tickets_quantity}</span>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Booked On</span>
              <span className="font-semibold text-gray-900">
                {format(parseISO(booking.created_at), 'MMM d, yyyy')}
              </span>
            </div>
          </div>

          {/* Attendee Information */}
          <div className="mt-8">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Attendee Information</h3>
            <div className="bg-gray-50 rounded-lg p-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                  <User className="w-5 h-5 text-blue-600" />
                </div>
                <div>
                  <div className="font-medium text-gray-900">{profile.full_name}</div>
                  <div className="text-sm text-gray-500">{profile.email}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Event Details */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Event Details</h2>
          
          <div className="space-y-6">
            <div>
              <h3 className="font-semibold text-gray-900 text-lg mb-2">{event.title}</h3>
              <p className="text-gray-600">{event.description}</p>
            </div>

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
        </div>
      </div>

      {/* QR Code Section */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="text-center">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Event Check-in</h2>
          <p className="text-gray-600 mb-6">Present this QR code at the event for quick check-in</p>
          
          <div className="inline-block p-4 bg-white border-2 border-gray-200 rounded-lg">
            <img
              src={qrCodeUrl}
              alt="Event QR Code"
              className="w-48 h-48 mx-auto"
            />
          </div>
          
          <div className="mt-6 flex flex-col sm:flex-row gap-4 justify-center">
            <button
              onClick={downloadQRCode}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
            >
              <Download className="w-4 h-4 mr-2" />
              Download QR Code
            </button>
            
            <Link
              to={`/events/${event.id}`}
              className="inline-flex items-center px-4 py-2 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors duration-200"
            >
              View Event Details
            </Link>
          </div>
        </div>
      </div>

      {/* Next Steps */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-8">
        <h2 className="text-xl font-semibold text-blue-900 mb-4">What's Next?</h2>
        <div className="space-y-3 text-blue-800">
          <div className="flex items-center space-x-2">
            <Mail className="w-5 h-5" />
            <span>A confirmation email has been sent to {profile.email}</span>
          </div>
          <div className="flex items-center space-x-2">
            <Calendar className="w-5 h-5" />
            <span>Add this event to your calendar to get reminders</span>
          </div>
          <div className="flex items-center space-x-2">
            <QrCode className="w-5 h-5" />
            <span>Save or screenshot the QR code for event check-in</span>
          </div>
        </div>
        
        <div className="mt-6 flex flex-col sm:flex-row gap-4">
          <Link
            to="/calendar"
            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
          >
            <Calendar className="w-4 h-4 mr-2" />
            View My Calendar
          </Link>
          
          <Link
            to="/dashboard"
            className="inline-flex items-center px-4 py-2 border border-blue-300 text-blue-700 font-medium rounded-lg hover:bg-blue-100 transition-colors duration-200"
          >
            Browse More Events
          </Link>
        </div>
      </div>
    </div>
  );
}