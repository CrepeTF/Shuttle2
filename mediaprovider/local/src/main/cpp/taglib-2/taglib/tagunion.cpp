﻿/***************************************************************************
    copyright            : (C) 2002 - 2008 by Scott Wheeler
    email                : wheeler@kde.org
 ***************************************************************************/

/***************************************************************************
 *   This library is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Lesser General Public License version   *
 *   2.1 as published by the Free Software Foundation.                     *
 *                                                                         *
 *   This library is distributed in the hope that it will be useful, but   *
 *   WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU     *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with this library; if not, write to the Free Software   *
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA         *
 *   02110-1301  USA                                                       *
 *                                                                         *
 *   Alternatively, this file is available under the Mozilla Public        *
 *   License Version 1.1.  You may obtain a copy of the License at         *
 *   http://www.mozilla.org/MPL/                                           *
 ***************************************************************************/

#include <tagunion.h>
#include <tstringlist.h>
#include <tpropertymap.h>
#include <tpicturemap.h>
#include <tsmartptr.h>

#define stringUnion(method)                                               \
  for(size_t j = 0; j < COUNT; ++j) {                                     \
    String val = d->tags[j] ? d->tags[j]->method() : String();            \
    if(!val.isEmpty())                                                    \
      return val;                                                         \
  }                                                                       \
  return String();

#define numberUnion(method)                                               \
  for(size_t j = 0; j < COUNT; ++j) {                                     \
    unsigned int val = d->tags[j] ? d->tags[j]->method() : 0;             \
    if(val > 0)                                                           \
      return val;                                                         \
  }                                                                       \
  return 0;

#define pictureMapUnion(method)                                           \
  for(size_t j = 0; j < COUNT; ++j) {                                     \
    PictureMap val = d->tags[j] ? d->tags[j]->method() : PictureMap();    \
    if(!val.isEmpty())                                                    \
      return val;                                                         \
  }                                                                       \
  return PictureMap();                                                    \

#define setUnion(method, value)                                           \
  for(size_t j = 0; j < COUNT; ++j) {                                     \
    if(d->tags[j])                                                        \
      d->tags[j]->set##method(value);                                     \
  }

namespace TagLib
{
  template <size_t COUNT>
  class TagUnion<COUNT>::TagUnionPrivate
  {
  public:
    SCOPED_PTR<Tag> tags[COUNT];
  };

  template <size_t COUNT>
  TagUnion<COUNT>::TagUnion()
    : d(new TagUnionPrivate())
  {
  }

  template <size_t COUNT>
  TagUnion<COUNT>::~TagUnion()
  {
    delete d;
  }

  template <size_t COUNT>
  Tag *TagUnion<COUNT>::operator[](size_t index) const
  {
    return tag(index);
  }

  template <size_t COUNT>
  Tag *TagUnion<COUNT>::tag(size_t index) const
  {
    return d->tags[index].get();
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::set(size_t index, Tag *tag)
  {
    d->tags[index].reset(tag);
  }

  template <size_t COUNT>
  PropertyMap TagUnion<COUNT>::properties() const
  {
    for(size_t i = 0; i < COUNT; ++i) {
      if(d->tags[i] && !d->tags[i]->isEmpty())
        return d->tags[i]->properties();
    }

    return PropertyMap();
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::removeUnsupportedProperties(const StringList &unsupported)
  {
    for(size_t i = 0; i < COUNT; ++i) {
      if(d->tags[i])
        d->tags[i]->removeUnsupportedProperties(unsupported);
    }
  }

  template <size_t COUNT>
  String TagUnion<COUNT>::title() const
  {
    stringUnion(title);
  }

  template <size_t COUNT>
  String TagUnion<COUNT>::artist() const
  {
    stringUnion(artist);
  }

  template <size_t COUNT>
  String TagUnion<COUNT>::album() const
  {
    stringUnion(album);
  }

  template <size_t COUNT>
  String TagUnion<COUNT>::comment() const
  {
    stringUnion(comment);
  }

  template <size_t COUNT>
  String TagUnion<COUNT>::genre() const
  {
    stringUnion(genre);
  }

  template <size_t COUNT>
  unsigned int TagUnion<COUNT>::year() const
  {
    numberUnion(year);
  }

  template <size_t COUNT>
  unsigned int TagUnion<COUNT>::track() const
  {
    numberUnion(track);
  }

  template <size_t COUNT>
  unsigned int TagUnion<COUNT>::trackTotal() const
  {
    numberUnion(trackTotal);
  }

    template <size_t COUNT>
    String TagUnion<COUNT>::trackTot() const
    {
        stringUnion(trackTot);
    }

  template <size_t COUNT>
  void TagUnion<COUNT>::setTitle(const String &s)
  {
    setUnion(Title, s);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setArtist(const String &s)
  {
    setUnion(Artist, s);
  }

  template <size_t COUNT>
  TagLib::PictureMap TagUnion<COUNT>::pictures() const
  {
    pictureMapUnion(pictures);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setAlbum(const String &s)
  {
    setUnion(Album, s);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setComment(const String &s)
  {
    setUnion(Comment, s);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setGenre(const String &s)
  {
    setUnion(Genre, s);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setYear(unsigned int i)
  {
    setUnion(Year, i);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setTrack(unsigned int i)
  {
    setUnion(Track, i);
  }

  template <size_t COUNT>
  void TagUnion<COUNT>::setPictures(const PictureMap &l)
  {
      setUnion(Pictures, l);
  }

  template <size_t COUNT>
  bool TagUnion<COUNT>::isEmpty() const
  {
    for(size_t i = 0; i < COUNT; ++i) {
      if(d->tags[i] && !d->tags[i]->isEmpty())
        return false;
    }

    return true;
  }

  // All the versions of TagUnion should be explicitly instantiated here.

  template class TagUnion<2>;
  template class TagUnion<3>;
}
